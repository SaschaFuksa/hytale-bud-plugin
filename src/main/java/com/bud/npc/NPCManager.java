package com.bud.npc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.hypixel.hytale.server.npc.entities.NPCEntity;

import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudKweebecData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
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

}