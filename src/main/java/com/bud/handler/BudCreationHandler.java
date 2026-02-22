package com.bud.handler;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.components.PlayerBudComponent;
import com.bud.events.BudCreationEvent;
import com.bud.npc.BudManager;
import com.bud.npc.creation.BudSpawner;
import com.bud.profile.BudProfileMapper;
import com.bud.profile.BudType;
import com.bud.profile.IBudProfile;
import com.bud.reaction.state.BudState;
import com.bud.scheduling.StateChangeCache;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.Pair;

public class BudCreationHandler implements Consumer<BudCreationEvent> {

    @Override
    public void accept(BudCreationEvent event) {
        if (!event.playerRef().isValid())
            return;
        Store<EntityStore> store = event.playerRef().getStore();
        PlayerRef playerRef = store.getComponent(event.playerRef(), PlayerRef.getComponentType());
        if (playerRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid PlayerRef provided in BudCreationEvent.");
            return;
        }
        PlayerBudComponent playerBudComponent = store.getComponent(event.playerRef(),
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] PlayerBudComponent not found for player.");
            return;
        }
        for (BudType budType : event.budTypes()) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Bud of type " + budType);
            if (budType == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Invalid BudType provided: " + budType);
                continue;

            }
            this.createBud(store, playerRef, budType, playerBudComponent);
        }
    }

    private void createBud(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            @Nonnull BudType budType, @Nonnull PlayerBudComponent playerBudComponent) {
        if (BudManager.playerHasValidBud(playerBudComponent, budType)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Player already has Bud of type " + budType);
            return;
        }
        NPCEntity bud = spawnBud(store, playerRef, budType);
        if (bud == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to spawn Bud of type " + budType);
            return;
        }
        playerBudComponent.addBud(bud, budType);
        registerBudComponent(store, bud, playerRef);
        StateChangeCache.getInstance()
                .addToCache(new StateChangeCache.StateChangeEntry(bud, playerRef, BudState.PET_DEFENSIVE));
    }

    private static NPCEntity spawnBud(Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            BudType budType) {
        IBudProfile budProfile = BudProfileMapper.getInstance().getProfileForBudType(budType);
        Vector3d position = BudManager.getInstance().getPlayerPositionWithOffset(playerRef);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = BudSpawner
                .create(store, budType.getName(), position)
                .withRotation(new Vector3f(0, 0, 0))
                .withInventory()
                .addWeapon(budProfile.getWeaponID(), 1, (short) 0)
                .addArmor(budProfile.getArmorID())
                .spawn();
        return (NPCEntity) result.second();
    }

    private void registerBudComponent(@Nonnull Store<EntityStore> store, NPCEntity bud, @Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid NPCEntity reference for bud: " + bud);
            return;
        }
        BudComponent budComponent = new BudComponent(bud, playerRef, BudState.PET_DEFENSIVE.getStateName());
        store.addComponent(ref, BudComponent.getComponentType(), budComponent);
    }
}
