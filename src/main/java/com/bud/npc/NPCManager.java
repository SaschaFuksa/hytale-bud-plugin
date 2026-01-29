package com.bud.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.lang.reflect.Field;

import com.bud.BudConfig;
import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class NPCManager {

    public static final ConcurrentHashMap<UUID, Set<NPCEntity>> spawnedBuds = new ConcurrentHashMap<>();
    private static NPCStateTracker stateTracker;

    private NPCManager(BudConfig config) {
        NPCManager.stateTracker = new NPCStateTracker(config);
    }
    
    private static final Set<IBudNPCData> BUDS = Set.of(
        new BudFeranData(),
        new BudTrorkData()
    );

    public static NPCManager getInstance(BudConfig config) {
        return new NPCManager(config);
    }
    
    public static Set<IBudNPCData> getMissingBuds(UUID playerId, Store<EntityStore> store) {
        Set<IBudNPCData> missingBuds = BUDS.stream().collect(Collectors.toSet());
        if (!spawnedBuds.containsKey(playerId) || spawnedBuds.get(playerId).isEmpty()) {
            return missingBuds;
        }
        Set<NPCEntity> registeredBuds = spawnedBuds.get(playerId);
        for (NPCEntity bud : registeredBuds) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef != null) {
                boolean isDead = store.getArchetype(budRef).contains(DeathComponent.getComponentType());
                if (!isDead && budRef.isValid()) {
                    switch (bud.getNPCTypeId()) {
                        case BudTrorkData.NPC_TYPE_ID -> missingBuds.remove(new BudTrorkData());
                        case BudFeranData.NPC_TYPE_ID -> missingBuds.remove(new BudFeranData());
                        default -> throw new AssertionError();
                    }
                }
            }
        }
        return missingBuds;
    }

    public static void addSpawnedBud(PlayerRef playerRef, IBudNPCData npcData) {
        spawnedBuds.putIfAbsent(playerRef.getUuid(), new HashSet<>());
        spawnedBuds.get(playerRef.getUuid()).add(npcData.getNPC());
        stateTracker.trackBud(playerRef, npcData.getNPC(), npcData);
    }
    
    public Set<String> getTrackedBudTypes() {
        Set<String> types = new HashSet<>();
        for (IBudNPCData budData : BUDS) {
            types.add(budData.getNPCTypeId());
        }
        return types;
    }
    
    public Set<Ref<EntityStore>> getTrackedBudRefs() {
        Set<Ref<EntityStore>> refs = new HashSet<>();
        for (Set<NPCEntity> npcs : spawnedBuds.values()) {
            for (NPCEntity npc : npcs) {
                if (npc == null) {
                    continue;
                }
                Ref<EntityStore> ref = npc.getReference();
                if (ref != null && ref.isValid()) {
                    refs.add(ref);
                }
            }
        }
        return refs;
    }

    public void removeBudForOwner(UUID ownerId) {
        Set<NPCEntity> buds = spawnedBuds.remove(ownerId);
        this.stateTracker.untrackBud(ownerId);

        for (NPCEntity bud : buds) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null) {
                continue;
            }
            Store<EntityStore> store = budRef.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> store.removeEntity(budRef, RemoveReason.REMOVE));
            System.out.println("[BUD] Removed Bud for player " + ownerId);
        }
    }

        /**
     * Iterates over all spawned buds for the player and forces them to be friendly
     * towards each other using the overrideAttitude system.
     */
    public static void makeAllBudsFriendly(PlayerRef playerRef) {
        Set<NPCEntity> buds = NPCManager.spawnedBuds.get(playerRef.getUuid());
        if (buds == null || buds.isEmpty()) {
            return;
        }
        
        System.out.println("[BUD] Updating attitudes for " + buds.size() + " buds...");
        
        for (NPCEntity source : buds) {
            for (NPCEntity target : buds) {
                // Don't set attitude towards self
                if (source == target) {
                    continue;
                }
                
                try {
                    // Check if both entities are valid
                    if (source.getRole() != null && source.getReference() != null && source.getReference().isValid() &&
                        target.getReference() != null && target.getReference().isValid()) {
                        
                        WorldSupport worldSupport = source.getRole().getWorldSupport();
                        
                        // FIX: Ensure memory map is initialized via reflection if missing (needed because JSON role config doesn't enable it)
                        try {
                            Field field = WorldSupport.class.getDeclaredField("attitudeOverrideMemory");
                            field.setAccessible(true);
                            if (field.get(worldSupport) == null) {
                                field.set(worldSupport, new Int2ObjectOpenHashMap<>());
                            }
                        } catch (Exception ex) {
                            System.out.println("[BUD] Failed to init override memory: " + ex.getMessage());
                        }

                        // Force source to be friendly to target
                        worldSupport.overrideAttitude(
                            target.getReference(),
                            Attitude.FRIENDLY,
                            Double.MAX_VALUE  // Permanent override
                        );
                        
                        System.out.println("[BUD] Forced " + source.getNPCTypeId() + " to be FRIENDLY to " + target.getNPCTypeId());
                    }
                } catch (Exception e) {
                    System.out.println("[BUD] Error setting attitude: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
}
