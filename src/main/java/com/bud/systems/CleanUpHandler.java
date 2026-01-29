package com.bud.systems;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.bud.npc.NPCManager;
import com.bud.npcdata.BudPlayerData;
import com.bud.npcdata.BudPlayerData.StoredBud;
import com.bud.npcdata.IBudNPCData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import com.bud.BudPlugin;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.entity.Entity;

public class CleanUpHandler {

    public static void cleanOrphanedBuds(PlayerRef player, World world) {
        System.out.println("[BUD] Cleanup: Remove orphans for " + player.getUuid());
        if (world == null) return;

        Holder<EntityStore> holder = player.getHolder();
        if (holder != null && holder.getComponent(BudPlugin.BUD_PLAYER_DATA) != null) {
            BudPlayerData data = holder.getComponent(BudPlugin.BUD_PLAYER_DATA);
            List<StoredBud> stored = data.getStoredBuds();

            if (stored.isEmpty()) return;

            System.out.println("[BUD] Restore/Cleanup: Checking " + stored.size() + " persistent Buds for " + player.getUuid());
            
            List<StoredBud> toRemove = new ArrayList<>();
            
            for (StoredBud budInfo : stored) {
                Entity entity = world.getEntity(budInfo.uuid);
                
                if (entity != null && entity.getReference() != null && entity.getReference().isValid()) {
                     // It exists! Restore tracking!
                    Ref<EntityStore> ref = entity.getReference();
                    if (ref.getStore().getComponent(ref, NPCEntity.getComponentType()) != null) {
                        NPCEntity npc = ref.getStore().getComponent(ref, NPCEntity.getComponentType());
                        
                        String typeId = npc.getNPCTypeId();
                        IBudNPCData prototype = NPCManager.getInstance().getDataForType(typeId);
                        if (prototype != null) {
                            System.out.println("[BUD] Restoring session for Bud: " + typeId);
                            NPCManager.getInstance().getStateTracker().trackBud(player, npc, prototype);
                        } else {
                            System.out.println("[BUD] Unknown Bud type " + typeId + " - removing.");
                            toRemove.add(budInfo);
                        }
                    }
                } else {
                    // Entity not found in world (despawned or unloaded)
                    System.out.println("[BUD] Bud " + budInfo.uuid + " not found in world. Removed from persistence.");
                    toRemove.add(budInfo);
                }
            }
            
            // cleanup invalid entries
            if (!toRemove.isEmpty()) {
                for(StoredBud b : toRemove) {
                    data.removeBud(b.uuid);
                }
                // Component is mutable, no need to setComponent if it doesn't exist
            }
        }
    }

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

    public static void cleanOrphanedBuds(World world) {
        NPCManager manager = NPCManager.getInstance();
        Set<Ref<EntityStore>> refsSnapshot = manager.getTrackedBudRefs();
        Set<String> typesSnapshot = manager.getTrackedBudTypes();

        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> cleanupWorld(world, refsSnapshot, typesSnapshot),
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
