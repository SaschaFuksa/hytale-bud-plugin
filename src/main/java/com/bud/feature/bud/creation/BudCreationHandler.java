package com.bud.feature.bud.creation;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.debug.BudDebugInfo;
import com.bud.core.types.BudState;
import com.bud.core.types.BudType;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.queue.state.StateChangeEntry;
import com.bud.feature.queue.state.StateChangeQueue;
import com.bud.feature.teleport.TeleportEvent;
import com.bud.llm.profiles.IBudProfile;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.Pair;

public class BudCreationHandler implements Consumer<BudCreationEvent> {

    @Override
    public void accept(BudCreationEvent event) {
        if (!event.playerRef().isValid())
            return;
        World world = event.playerRef().getStore().getExternalData().getWorld();
        world.execute(() -> this.handleEvent(event));
    }

    private void handleEvent(BudCreationEvent event) {
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
            playerBudComponent.getCurrentBuds().stream()
                    .filter(b -> b.getNPCTypeId().equals(budType.getName()))
                    .findFirst()
                    .ifPresent(bud -> {
                        BudComponent budComponent = BudManager.getInstance().getBudComponent(bud);
                        TeleportEvent.dispatch(store, budComponent);
                        LoggerUtil.getLogger()
                                .fine(() -> "[BUD] Teleported existing Bud of type " + budType + " for player "
                                        + playerBudComponent.getPlayerRef().getUsername());
                    });
            return;
        }
        NPCEntity bud = spawnBud(store, playerRef, budType);
        if (bud == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to spawn Bud of type " + budType);
            return;
        }
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Successfully spawned Bud with NPC Type ID: " + bud.getNPCTypeId());
        playerBudComponent.addBud(bud, budType);
        BudComponent budComponent = registerBudComponent(store, bud, playerRef, budType);
        if (budComponent == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to register BudComponent for Bud of type " + budType);
            return;
        }
        StateChangeQueue.getInstance()
                .addToCache(new StateChangeEntry(BudState.PET_DEFENSIVE, budComponent));
        if (DebugConfig.getInstance().isEnableBudDebugInfo()) {
            BudDebugInfo.getInstance().logBudInfo(bud);
        }

    }

    private static NPCEntity spawnBud(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            @Nonnull BudType budType) {
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

    private BudComponent registerBudComponent(@Nonnull Store<EntityStore> store, NPCEntity bud,
            @Nonnull PlayerRef playerRef, @Nonnull BudType budType) {
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid NPCEntity reference for bud: " + bud);
            return null;
        }
        BudComponent budComponent = BudComponent.create(bud, budType, playerRef);
        store.addComponent(ref, BudComponent.getComponentType(), budComponent);
        return budComponent;
    }
}
