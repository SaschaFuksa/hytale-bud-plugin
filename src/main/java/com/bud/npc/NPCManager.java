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
import com.bud.result.DataListResult;
import com.bud.result.DataResult;
import com.bud.result.ErrorResult;
import com.bud.result.IDataListResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class NPCManager {

    private static final NPCManager INSTANCE = new NPCManager();

    private NPCManager() {
    }

    public static NPCManager getInstance() {
        return INSTANCE;
    }

    private static final Set<IBudNPCData> BUDS = Set.of(
            new BudFeranData(),
            new BudTrorkData(),
            new BudKweebecData());

    public IBudNPCData getDataForType(String typeId) {
        for (IBudNPCData data : BUDS) {
            if (data.getNPCTypeId().equals(typeId)) {
                return data;
            }
        }
        return null; // Or unknown
    }

    public Set<IBudNPCData> getMissingBuds(UUID playerId, Store<EntityStore> store) {
        Set<IBudNPCData> missingBuds = BUDS.stream().collect(Collectors.toSet());
        Set<BudInstance> playerBuds = BudRegistry.getInstance().getByOwner(playerId);

        if (playerBuds.isEmpty()) {
            return missingBuds;
        }

        for (BudInstance instance : playerBuds) {
            Ref<EntityStore> budRef = instance.getRef();
            if (budRef == null)
                continue;

            boolean isDead = store.getArchetype(budRef).contains(DeathComponent.getComponentType());
            if (!isDead && budRef.isValid()) {
                String typeId = instance.getData().getNPCTypeId();
                missingBuds.removeIf(b -> b.getNPCTypeId().equals(typeId));
            }
        }
        return missingBuds;
    }

    public boolean canBeAdded(UUID playerId, Store<EntityStore> store, IBudNPCData npcData) {
        Set<IBudNPCData> missingBuds = getMissingBuds(playerId, store);
        return missingBuds.stream()
                .anyMatch(b -> b.getNPCTypeId().equals(npcData.getNPCTypeId()));
    }

    public IResult persistBud(@Nonnull PlayerRef playerRef, @Nonnull NPCEntity npc) {
        System.out.println("[BUD] Persist data for " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            BudPlayerData customData = store.ensureAndGetComponent(ref,
                    BudPlugin.getInstance().getBudPlayerDataComponent());
            customData.add(npc.getUuid());
            store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), customData);
            return new SuccessResult("Data persisted for " + playerRef.getUuid() + " via store.putComponent");
        } catch (Exception e) {
        }
        return new ErrorResult("Not able to persist data for " + playerRef.getUuid());
    }

    public IDataListResult<UUID> getPersistedBudUUIDs(@Nonnull PlayerRef playerRef) {
        System.out.println("[BUD] Get persisted data for " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            BudPlayerData customData = store.ensureAndGetComponent(ref,
                    BudPlugin.getInstance().getBudPlayerDataComponent());
            Set<UUID> budUUIDs = customData.getBuds();
            return new DataListResult<>(budUUIDs, "Retrieved persisted data for " + playerRef.getUuid());
        } catch (Exception e) {
            return new DataListResult<>(new HashSet<>(),
                    "Exception while retrieving persisted data for " + playerRef.getUuid() + ": " + e.getMessage());
        }
    }

    public IDataResult<NPCEntity> getNPCEntityByUUID(@Nonnull UUID npcUUID, @Nonnull World world) {
        try {
            EntityStore entityStore = world.getEntityStore();
            Store<EntityStore> store = entityStore.getStore();
            Ref<EntityStore> ref = entityStore.getRefFromUUID(npcUUID);
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            return new DataResult<>(npc, "Found NPCEntity for UUID " + npcUUID);
        } catch (Exception e) {
            return new DataResult<>(null, "Failed to find NPCEntity for UUID " + npcUUID);
        }
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
        for (BudInstance instance : buds) {
            if (i == item)
                return instance.getEntity();
            i++;
        }
        return null;
    }

    public Vector3d getPlayerPosition(PlayerRef playerRef) {
        return playerRef.getTransform().getPosition();
    }

    public Set<UUID> getOwnedBudUUIDs(UUID ownerId) {
        Set<BudInstance> buds = BudRegistry.getInstance().getByOwner(ownerId);
        Set<UUID> budUUIDs = new HashSet<>();
        for (BudInstance bud : buds) {
            budUUIDs.add(bud.getEntity().getUuid());
        }
        return budUUIDs;
    }

    public IResult unpersistData(@Nonnull PlayerRef playerRef, UUID uuid) {
        System.out.println("[BUD] Unpersist data for " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            System.out.println("[BUD] Got player ref");
            Store<EntityStore> store = ref.getStore();
            System.out.println("[BUD] Got store");
            BudPlayerData customData = store.ensureAndGetComponent(ref,
                    BudPlugin.getInstance().getBudPlayerDataComponent());
            System.out.println("[BUD] Got custom data");
            customData.remove(uuid);
            System.out.println("[BUD] Removed NPC UUID " + uuid + " from player " + playerRef.getUuid());
            store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), customData);
            return new SuccessResult("Data unpersisted for " + playerRef.getUuid());
        } catch (Exception e) {
            return new ErrorResult("Not able to unpersist data for " + playerRef.getUuid());
        }
    }

}