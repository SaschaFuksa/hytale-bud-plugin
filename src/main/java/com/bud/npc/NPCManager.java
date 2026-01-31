package com.bud.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bud.BudPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudKweebecData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.bud.npcdata.persistence.BudPlayerData;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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

    public IBudNPCData getDataForType(String typeId) {
        for (IBudNPCData data : BUDS) {
            if (data.getNPCTypeId().equals(typeId)) {
                return data;
            }
        }
        return null; // Or unknown
    }

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

    public static boolean canBeAdded(UUID playerId, Store<EntityStore> store, IBudNPCData npcData) {
        Set<IBudNPCData> missingBuds = getMissingBuds(playerId, store);
        return missingBuds.stream()
                .anyMatch(b -> b.getNPCTypeId().equals(npcData.getNPCTypeId()));
    }

    public static IResult addSpawnedBud(PlayerRef playerRef, IBudNPCData npcData, NPCEntity npc) {
        IResult result = stateTracker.trackBud(playerRef, npc, npcData);
        if (!result.isSuccess()) {
            return result;
        }
        return persistData(playerRef, npc);
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

    public static Vector3d getPlayerPosition(PlayerRef playerRef) {
        return playerRef.getTransform().getPosition();
    }

    @SuppressWarnings("removal")
    public IResult unpersistData(@Nonnull PlayerRef playerRef, NPCEntity npc) {
        System.out.println("[BUD] Unpersist data for " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            System.out.println("[BUD] Got player ref");
            Store<EntityStore> store = ref.getStore();
            System.out.println("[BUD] Got store");
            BudPlayerData customData = store.ensureAndGetComponent(ref, BudPlugin.instance().getBudPlayerDataComponent());
            System.out.println("[BUD] Got custom data");
            customData.remove(npc.getUuid());
            System.out.println("[BUD] Removed NPC UUID " + npc.getUuid() + " from player " + playerRef.getUuid());
            store.putComponent(ref, BudPlugin.instance().getBudPlayerDataComponent(), customData);
            return new SuccessResult("Data unpersisted for " + playerRef.getUuid());
        } catch (Exception e) {
            return new ErrorResult("Not able to unpersist data for " + playerRef.getUuid());
        }
    }
    
    @SuppressWarnings("removal")
    private static IResult persistData(@Nonnull PlayerRef playerRef, @Nonnull NPCEntity npc) {
        System.out.println("[BUD] Persist data for " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            BudPlayerData customData = store.ensureAndGetComponent(ref, BudPlugin.instance().getBudPlayerDataComponent());
            customData.add(npc.getUuid());
            store.putComponent(ref, BudPlugin.instance().getBudPlayerDataComponent(), customData);
            return new SuccessResult("Data persisted for " + playerRef.getUuid() + " via store.putComponent");
        } catch (Exception e) {
        }
        return new ErrorResult("Not able to persist data for " + playerRef.getUuid());
    }
}