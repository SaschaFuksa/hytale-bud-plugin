package com.bud.feature.teleport;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudDefinition;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.BudState;
import com.bud.feature.bud.creation.BudSpawner;
import com.bud.feature.queue.teleport.TeleportEntry;
import com.bud.feature.queue.teleport.TeleportQueue;
import com.bud.feature.state.StateChangeEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.Pair;

public class TeleportHandler implements Consumer<TeleportEvent> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final long TELEPORT_DELAY_MS = 250;

    @Override
    public void accept(TeleportEvent event) {
        World world = event.store().getExternalData().getWorld();
        scheduleOnWorldThread(world, TELEPORT_DELAY_MS, () -> teleportBud(event));
    }

    private void scheduleOnWorldThread(World world, long delayMs, Runnable task) {
        SCHEDULER.schedule(() -> world.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] Exception in scheduled teleport task: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }), delayMs, TimeUnit.MILLISECONDS);
    }

    private void teleportBud(TeleportEvent event) {
        BudComponent budComponent = event.budComponent();
        PlayerRef playerRef = budComponent.getPlayerRef();
        Store<EntityStore> store = event.store();

        if (budComponent.getCurrentState() == BudState.WORKING) {
            // A working Bud stays at its workstation - never pulled to the player via waystone/`/bud
            // create` follow-teleports. Primary guard: callers (BudCreationHandler, TeleportFilterSystem)
            // also skip queueing Working Buds for teleport, this is defense-in-depth for any future
            // caller. See docs/bud-worker-mode-plan.md, "Working-State / Kampf-Lock".
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Skipping teleport for working Bud " + budComponent.getBudId());
            return;
        }

        NPCEntity oldBud = budComponent.getBud();
        Ref<EntityStore> oldRef = oldBud.getReference();
        if (oldRef == null || !oldRef.isValid()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid reference for bud " + budComponent.getBudId()
                            + " for player " + playerRef.getUsername());
            return;
        }

        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid player reference for teleport of bud " + budComponent.getBudId());
            return;
        }
        PlayerBudComponent playerBudComponent = store.getComponent(playerEntityRef,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            LoggerUtil.getLogger()
                    .warning(
                            () -> "[BUD] PlayerBudComponent not found for teleport of bud " + budComponent.getBudId());
            return;
        }

        Vector3d targetPos = BudManager.getInstance().getSpawnPosition(playerRef, 0, 1);
        Vector3f rotation = BudManager.getInstance().getRotationFacingPlayer(playerRef, targetPos);

        store.removeEntity(oldRef, RemoveReason.REMOVE);
        playerBudComponent.removeCurrentBud(oldBud, budComponent.getBudId());

        BudDefinition budProfile = BudRegistry.getInstance().get(budComponent.getBudId());
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = BudSpawner
                .create(store, budProfile.getNpcTypeId(), targetPos)
                .withRotation(rotation)
                .withInventory()
                .addWeapon(budProfile.getWeaponId(), 1, (short) 0)
                .addArmor(budProfile.getArmorId())
                .spawn();
        if (result == null || result.first() == null) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Failed to respawn bud " + budComponent.getBudId()
                            + " after teleport for player " + playerRef.getUsername());
            return;
        }

        NPCEntity newBud = Objects.requireNonNull((NPCEntity) result.second());
        Ref<EntityStore> newRef = Objects.requireNonNull(result.first());
        budComponent.setBud(newBud);
        store.addComponent(newRef, BudComponent.getComponentType(), budComponent);
        playerBudComponent.addBud(newBud, budComponent.getBudId());
        StateChangeEvent.dispatch(newBud, playerRef, budComponent.getCurrentState());

        if (event.shouldSendReaction()) {
            TeleportQueue.getInstance().addToCache(new TeleportEntry(budComponent, store));
        }
        LoggerUtil.getLogger()
                .info(() -> "[BUD] Respawned bud " + budComponent.getBudId() + " after teleport for player "
                        + playerRef.getUsername());
    }

}
