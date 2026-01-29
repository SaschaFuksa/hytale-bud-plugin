package com.bud.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudKweebecData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class NPCManager {

    public static final ConcurrentHashMap<UUID, Set<NPCEntity>> spawnedBuds = new ConcurrentHashMap<>();
    private static NPCStateTracker stateTracker;

    private NPCManager() {
        NPCManager.stateTracker = new NPCStateTracker();
    }
    
    private static final Set<IBudNPCData> BUDS = Set.of(
        new BudFeranData(),
        new BudTrorkData(),
        new BudKweebecData()
    );

    public static NPCManager getInstance() {
        return new NPCManager();
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
        System.out.println("[BUD] Removing Buds for owner " + ownerId);
        this.stateTracker.untrackBud(ownerId);
        Set<NPCEntity> buds = spawnedBuds.get(ownerId);
        System.err.println("[BUD] Found " + (buds != null ? buds.size() : 0) + " Buds to remove.");

        for (NPCEntity bud : buds) {
            Ref<EntityStore> budRef = bud.getReference();
            System.err.println("[BUD] Processing Bud with ref: " + budRef);

            if (budRef == null) {
                continue;
            }
            Store<EntityStore> store = budRef.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> store.removeEntity(budRef, RemoveReason.REMOVE));
            System.out.println("[BUD] Removed Bud for player " + ownerId);
        }
        spawnedBuds.remove(ownerId);
    }    
}
