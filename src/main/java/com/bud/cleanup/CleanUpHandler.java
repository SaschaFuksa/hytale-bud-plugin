package com.bud.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.npc.NPCManager;
import com.bud.npc.NPCStateTracker;
import com.bud.npc.persistence.PersistenceManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.result.ErrorResult;
import com.bud.result.IDataListResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class CleanUpHandler {

    public static IResult cleanupOwnerBuds(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        IDataListResult<UUID> persistedBudsResult = PersistenceManager.getInstance().getPersistedBudUUIDs(playerRef);
        persistedBudsResult.printResult();
        if (!persistedBudsResult.isSuccess()) {
            return persistedBudsResult;
        }
        List<UUID> persistedBuds = persistedBudsResult.getDataList();
        LoggerUtil.getLogger().fine(() -> "[BUD] Size of persisted buds: " + persistedBuds.size());
        List<String> errors = new ArrayList<>();
        for (UUID budUUID : persistedBuds) {
            IResult result = cleanupBud(playerRef, world, budUUID);
            if (!result.isSuccess()) {
                errors.add(result.getMessage());
            }
        }
        persistedBudsResult = PersistenceManager.getInstance().getPersistedBudUUIDs(playerRef);
        final int sizeAfterCleanup = persistedBudsResult.getDataList().size();
        LoggerUtil.getLogger().fine(() -> "[BUD] Size of persisted buds after cleanup: " + sizeAfterCleanup);
        if (!errors.isEmpty()) {
            return new ErrorResult("Errors occurred while removing owner buds: " + String.join(", ", errors));
        }
        return new SuccessResult("Removed all owner buds.");
    }

    public static IResult cleanupBud(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull UUID budUUID) {
        try {
            IDataResult<NPCEntity> npcResult = NPCManager.getInstance().getNPCEntityByUUID(budUUID, world);
            npcResult.printResult();
            if (!npcResult.isSuccess()) {
                LoggerUtil.getLogger().fine(() -> "[BUD] Maybe Entity already despawned. Try to unregister.");
            } else {
                NPCEntity npcEntity = npcResult.getData();
                NPCStateTracker.getInstance().unregisterBud(npcEntity).printResult();
                despawnBud(npcEntity).printResult();
            }

            IResult unpersistResult = PersistenceManager.getInstance().unpersistData(playerRef, budUUID);
            if (!unpersistResult.isSuccess()) {
                return unpersistResult;
            }
            return new SuccessResult("Removed owner bud successfully.");
        } catch (Exception e) {
            return new ErrorResult("Exception removing owner bud: " + e.getMessage());
        }
    }

    public static IResult cleanupAllBuds(World world) {
        Set<String> typesSnapshot = NPCManager.getInstance().getTrackedBudTypes();
        LoggerUtil.getLogger().fine(
                () -> "[BUD] Scheduling cleanup for world " + world.getName() + " with bud types: " + typesSnapshot);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> {
                    IResult result = cleanupWorld(world, typesSnapshot);
                    result.printResult();
                },
                1L,
                TimeUnit.SECONDS);
        return new SuccessResult("Scheduled cleanup for world " + world.getName());
    }

    public static IResult despawnBud(NPCEntity npcEntity) {
        try {
            Ref<EntityStore> entityRef = npcEntity.getReference();
            entityRef.getStore().removeEntity(entityRef, RemoveReason.REMOVE);
            return new SuccessResult("Entity removed successfully.");
        } catch (Exception e) {
            return new ErrorResult("Exception checking entity reference: " + e.getMessage());
        }
    }

    private static IResult cleanupWorld(World world, Set<String> trackedBudTypes) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Cleaning up world " + world.getName() + " for bud types: " + trackedBudTypes);
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            // Collection to hold tracked buds for safe unregister/unpersist
            ConcurrentLinkedQueue<BudInstance> trackedBudsToRemove = new ConcurrentLinkedQueue<>();

            world.execute(() -> {
                store.forEachEntityParallel(
                        NPCEntity.getComponentType(),
                        (index, archetypeChunk, commandBuffer) -> {
                            NPCEntity npcComponent = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
                            if (npcComponent == null || !trackedBudTypes.contains(npcComponent.getNPCTypeId())) {
                                return;
                            }

                            // Check if it's a tracked bud with an owner
                            BudInstance instance = BudRegistry.getInstance()
                                    .get(npcComponent.getReference());
                            if (instance != null) {
                                trackedBudsToRemove.add(instance);
                            } else {
                                // Orphan / Untracked: Remove immediately
                                commandBuffer.removeEntity(npcComponent.getReference(), RemoveReason.REMOVE);
                            }
                        });

                // Process tracked buds sequentially to ensure thread-safety for data
                // persistence
                for (BudInstance instance : trackedBudsToRemove) {
                    try {
                        NPCEntity npcEntity = instance.getEntity();
                        PlayerRef owner = instance.getOwner();

                        // Unregister from runtime tracker
                        NPCStateTracker.getInstance().unregisterBud(npcEntity).printResult();

                        // Despawn entity (removes from world)
                        despawnBud(npcEntity).printResult();

                        // Unpersist from player data
                        if (owner != null && npcEntity != null) {
                            PersistenceManager.getInstance().unpersistData(owner, npcEntity.getUuid()).printResult();
                        }
                    } catch (Exception e) {
                        LoggerUtil.getLogger().severe(() -> "[BUD] Error cleaning up tracked bud: " + e.getMessage());
                    }
                }
            });
            return new SuccessResult("Cleaned up world " + world.getName());
        } catch (Exception e) {
            return new ErrorResult("Exception during cleanup of world " + world.getName() + ": " + e.getMessage());
        }
    }

}
