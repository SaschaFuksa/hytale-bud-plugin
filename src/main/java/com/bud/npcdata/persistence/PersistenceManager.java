package com.bud.npcdata.persistence;

import java.util.HashSet;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.bud.BudPlugin;
import com.bud.result.DataListResult;
import com.bud.result.ErrorResult;
import com.bud.result.IDataListResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class PersistenceManager {

    private static final PersistenceManager INSTANCE = new PersistenceManager();

    private PersistenceManager() {
    }

    public static PersistenceManager getInstance() {
        return INSTANCE;
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
            Holder<EntityStore> holder = playerRef.getHolder();
            if (holder == null) {
                return getRefPersistedBudUUIDs(playerRef);
            }
            BudPlayerData customData = holder
                    .ensureAndGetComponent(BudPlugin.getInstance().getBudPlayerDataComponent());
            return new DataListResult<>(customData.getBuds(), "Retrieved persisted data for " +
                    playerRef.getUuid());
        } catch (Exception e) {
            return new DataListResult<>(new HashSet<>(),
                    "Exception while retrieving persisted data for " + playerRef.getUuid() + ": " + e.getMessage());
        }
    }

    public IResult unpersistData(@Nonnull PlayerRef playerRef, UUID uuid) {
        System.out.println("[BUD] Unpersist data for " + playerRef.getUuid());
        try {
            Holder<EntityStore> holder = playerRef.getHolder();
            if (holder == null) {
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
            } else {
                BudPlayerData customData = holder
                        .ensureAndGetComponent(BudPlugin.getInstance().getBudPlayerDataComponent());
                System.out.println("[BUD] Got custom data");
                customData.remove(uuid);
                System.out.println("[BUD] Removed NPC UUID " + uuid + " from player " + playerRef.getUuid());
                holder.putComponent(BudPlugin.getInstance().getBudPlayerDataComponent(), customData);
            }
            return new SuccessResult("Data unpersisted for " + playerRef.getUuid());
        } catch (Exception e) {
            return new ErrorResult("Not able to unpersist data for " + playerRef.getUuid());
        }
    }

    private IDataListResult<UUID> getRefPersistedBudUUIDs(@Nonnull PlayerRef playerRef) {
        System.out.println("[BUD] Get persisted data via ref for " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            BudPlayerData customData = store.ensureAndGetComponent(ref,
                    BudPlugin.getInstance().getBudPlayerDataComponent());
            return new DataListResult<>(customData.getBuds(), "Retrieved persisted data for " +
                    playerRef.getUuid() + " via ref");
        } catch (Exception e) {
            return new DataListResult<>(new HashSet<>(),
                    "Exception while retrieving persisted data for " + playerRef.getUuid() + " via ref: "
                            + e.getMessage());
        }
    }

}
