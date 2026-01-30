package com.bud.system;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.npc.NPCManager;
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
import com.bud.result.IResult;
import com.bud.result.SuccessResult;

public class CleanUpHandler {
    
    public static IResult removeOwnerBuds(PlayerRef playerRef) {
        BudRegistry instance = BudRegistry.getInstance();
        Set<BudInstance> buds = instance.getByOwner(playerRef.getUuid());
        for (BudInstance bud : buds) {
            IResult removeOwnerBudResult = removeOwnerBud(bud, playerRef);
            if (!removeOwnerBudResult.isSuccess()) {
                return removeOwnerBudResult;
            }
        }
        return new SuccessResult("Removed all owner buds.");
    }
    
    public static IResult removeAllBuds(World world) {
        NPCManager manager = NPCManager.getInstance();
        Set<Ref<EntityStore>> refsSnapshot = manager.getTrackedBudRefs();
        Set<String> typesSnapshot = manager.getTrackedBudTypes();
        
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> {
                IResult result = cleanupWorld(world, refsSnapshot, typesSnapshot);
                result.printResult();
            },
            1L,
            TimeUnit.SECONDS
        );
        return new SuccessResult("Scheduled cleanup for world " + world.getName());
    }

    private static IResult removeOwnerBud(BudInstance bud, PlayerRef playerRef) {
        IResult untrackResult = NPCManager.getInstance().getStateTracker().untrackBud(bud);
        if (!untrackResult.isSuccess()) {
            return untrackResult;
        }
        IResult unpersistResult = NPCManager.getInstance().unpersistData(playerRef, bud.getEntity());
        if (!unpersistResult.isSuccess()) {
            return unpersistResult;
        }
        IResult removeResult = removeEntity(bud.getEntity().getReference());
        if (!removeResult.isSuccess()) {
            return removeResult;
        }
        return new SuccessResult("Removed owner bud successfully.");
    }
    
    private static IResult cleanupWorld(World world, Set<Ref<EntityStore>> trackedBudRefs, Set<String> trackedBudTypes) {
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
                }
            ));
            return new SuccessResult("Cleaned up world " + world.getName());
        } catch (Exception e) {
            return new ErrorResult("Exception during cleanup of world " + world.getName() + ": " + e.getMessage());
        }     
    }
    
    private static IResult removeEntity(Ref<EntityStore> entityRef) {
        try {
            entityRef.getStore().removeEntity(entityRef, RemoveReason.REMOVE);
            return new SuccessResult("Entity removed successfully.");
        } catch (Exception e) {
            return new ErrorResult("Exception checking entity reference: " + e.getMessage());
        }
    }
    
}
