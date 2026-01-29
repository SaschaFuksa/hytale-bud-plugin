package com.bud.budworld;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.npc.NPCManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class CleanUpHandler {

    public static void removeAllBuds(World world) {
        NPCManager manager = NPCManager.getInstance();
        Set<Ref<EntityStore>> refsSnapshot = manager.getTrackedBudRefs();
        Set<String> typesSnapshot = manager.getTrackedBudTypes();

        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> cleanupAllWorlds(refsSnapshot, typesSnapshot),
            1L,
            TimeUnit.SECONDS
        );
    }
    
    public static void removeOwnerBuds(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        NPCManager.getInstance().removeBudForOwner(playerRef.getUuid());
    }

    private static void cleanupAllWorlds(Set<Ref<EntityStore>> trackedBudRefs, Set<String> trackedBudTypes) {
        Universe.get().getWorlds().forEach((name, world) -> cleanupWorld(world, trackedBudRefs, trackedBudTypes));
    }

    private static void cleanupWorld(World world, Set<Ref<EntityStore>> trackedBudRefs, Set<String> trackedBudTypes) {
        if (world == null) {
            return;
        }

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
        System.out.println("[BUD] Cleaned up orphan Buds in world " + world.getName());
    }

}
