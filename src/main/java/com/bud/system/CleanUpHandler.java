package com.bud.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.npc.NPCManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import com.bud.npc.NPCStateTracker;
import com.bud.result.ErrorResult;
import com.bud.result.IDataListResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;

public class CleanUpHandler {

    public static IResult cleanupOwnerBuds(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        IDataListResult<UUID> persistedBudsResult = NPCManager.getInstance().getPersistedBudUUIDs(playerRef);
        persistedBudsResult.printResult(); // REMOVE
        if (!persistedBudsResult.isSuccess()) {
            return persistedBudsResult;
        }
        List<UUID> persistedBuds = persistedBudsResult.getDataList();
        System.out.println("[BUD] size of persisted buds: " + persistedBuds.size());
        List<String> errors = new ArrayList<>();
        for (UUID budUUID : persistedBuds) {
            IResult result = cleanupBud(playerRef, world, budUUID);
            if (!result.isSuccess()) {
                errors.add(result.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return new ErrorResult("Errors occurred while removing owner buds: " + String.join(", ", errors));
        }
        return new SuccessResult("Removed all owner buds.");
    }

    private static IResult cleanupBud(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull UUID budUUID) {
        try {
            IDataResult<NPCEntity> npcResult = NPCManager.getInstance().getNPCEntityByUUID(budUUID, world);
            npcResult.printResult();
            if (!npcResult.isSuccess()) {
                System.err.println("[BUD] Maybe Entity already despawned. Try to unregister.");
            } else {
                NPCEntity npcEntity = npcResult.getData();
                NPCStateTracker.getInstance().unregisterBud(npcEntity).printResult();
                despawnBud(npcEntity).printResult();
            }

            IResult unpersistResult = NPCManager.getInstance().unpersistData(playerRef, budUUID);
            if (!unpersistResult.isSuccess()) {
                return unpersistResult;
            }
            return new SuccessResult("Removed owner bud successfully.");
        } catch (Exception e) {
            return new ErrorResult("Exception removing owner bud: " + e.getMessage());
        }
    }

    public static IResult cleanupAllBuds(World world) {
        NPCManager manager = NPCManager.getInstance();
        Set<Ref<EntityStore>> refsSnapshot = manager.getTrackedBudRefs();
        Set<String> typesSnapshot = manager.getTrackedBudTypes();

        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> {
                    IResult result = cleanupWorld(world, refsSnapshot, typesSnapshot);
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

    private static IResult cleanupWorld(World world, Set<Ref<EntityStore>> trackedBudRefs,
            Set<String> trackedBudTypes) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            world.execute(() -> store.forEachEntityParallel(
                    NPCEntity.getComponentType(),
                    (index, archetypeChunk, commandBuffer) -> {
                        NPCEntity npcComponent = archetypeChunk.getComponent(index, NPCEntity.getComponentType());
                        if (npcComponent == null) {
                            return;
                        }
                        if (!trackedBudTypes.contains(npcComponent.getNPCTypeId())) {
                            return;
                        }
                        Ref<EntityStore> npcRef = archetypeChunk.getReferenceTo(index);
                        if (!trackedBudRefs.contains(npcRef)) {
                            commandBuffer.removeEntity(npcRef, RemoveReason.REMOVE);
                        }
                    }));
            return new SuccessResult("Cleaned up world " + world.getName());
        } catch (Exception e) {
            return new ErrorResult("Exception during cleanup of world " + world.getName() + ": " + e.getMessage());
        }
    }

}
