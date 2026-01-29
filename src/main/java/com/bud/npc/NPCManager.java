package com.bud.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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

    private static final NPCStateTracker stateTracker = new NPCStateTracker();
    private static final NPCManager INSTANCE = new NPCManager();

    private NPCManager() {
    }
    
    private static final Set<IBudNPCData> BUDS = Set.of(
        new BudFeranData(),
        new BudTrorkData(),
        new BudKweebecData()
    );

    public static NPCManager getInstance() {
        return INSTANCE;
    }
    
    public static Set<IBudNPCData> getMissingBuds(UUID playerId, Store<EntityStore> store) {
        Set<IBudNPCData> missingBuds = BUDS.stream().collect(Collectors.toSet());
        Set<BudInstance> playerBuds = BudRegistry.getInstance().getByOwner(playerId);
        
        if (playerBuds.isEmpty()) {
            return missingBuds;
        }

        for (BudInstance instance : playerBuds) {
            Ref<EntityStore> budRef = instance.getRef();
            if (budRef == null) continue;
            
            boolean isDead = store.getArchetype(budRef).contains(DeathComponent.getComponentType());
            if (!isDead && budRef.isValid()) {
                String typeId = instance.getData().getNPCTypeId();
                missingBuds.removeIf(b -> b.getNPCTypeId().equals(typeId));
            }
        }
        return missingBuds;
    }

    public static void addSpawnedBud(PlayerRef playerRef, IBudNPCData npcData, NPCEntity npc) {
        stateTracker.trackBud(playerRef, npc, npcData);
    }
    
    public Set<String> getTrackedBudTypes() {
        Set<String> types = new HashSet<>();
        for (IBudNPCData budData : BUDS) {
            types.add(budData.getNPCTypeId());
        }
        return types;
    }
    
    public Set<Ref<EntityStore>> getTrackedBudRefs() {
        return BudRegistry.getInstance().getAllRefs();
    }

    public void removeBudForOwner(UUID ownerId) {
        System.out.println("[BUD] Removing Buds for owner " + ownerId);
        Set<BudInstance> buds = new HashSet<>(BudRegistry.getInstance().getByOwner(ownerId));
        System.err.println("[BUD] Found " + buds.size() + " Buds to remove.");
        
        for (BudInstance instance : buds) {
            Ref<EntityStore> budRef = instance.getRef();
            System.err.println("[BUD] Processing Bud with ref: " + budRef);
            
            if (budRef == null) {
                continue;
            }
            Store<EntityStore> store = budRef.getStore();
            if (store != null) {
                World world = store.getExternalData().getWorld();
                if (world != null) {
                    world.execute(() -> store.removeEntity(budRef, RemoveReason.REMOVE));
                }
            }
            stateTracker.untrackBud(instance);
        }
    }

    public boolean isBudOwnedBy(UUID playerUUID, Ref<EntityStore> npcRef) {
        BudInstance instance = BudRegistry.getInstance().get(npcRef);
        return instance != null && instance.getOwner().getUuid().equals(playerUUID);
    }

    public NPCEntity getRandomBud(UUID ownerId) {
        Set<BudInstance> buds = BudRegistry.getInstance().getByOwner(ownerId);
        
        if (buds.isEmpty()) {
            return null;
        }
        
        int size = buds.size();
        int item = new java.util.Random().nextInt(size);
        int i = 0;
        for(BudInstance instance : buds) {
            if (i == item)
                return instance.getEntity();
            i++;
        }
        return null;
    }

    public NPCStateTracker getStateTracker() {
        return stateTracker;
    }
}