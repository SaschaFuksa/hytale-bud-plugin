package com.bud.handler;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.components.PlayerBudComponent;
import com.bud.events.TeleportEvent;
import com.bud.npc.BudManager;
import com.bud.profile.BudType;
import com.bud.queue.teleport.TeleportEntry;
import com.bud.queue.teleport.TeleportQueue;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class TeleportHandler implements Consumer<TeleportEvent> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final long TELEPORT_DELAY_MS = 25;

    @Override
    public void accept(TeleportEvent event) {
        for (BudType budType : event.budTypes()) {
            SCHEDULER.schedule(() -> {
                event.store().getExternalData().getWorld().execute(() -> {
                    this.teleportBud(event, budType);
                });
            }, TELEPORT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    public static void handleTeleport(@Nonnull Store<EntityStore> store, @Nonnull PlayerBudComponent playerBudComponent,
            @Nonnull BudType budType) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Handling teleport for player " + playerBudComponent.getPlayerRef().getUsername()
                        + " with Bud type: " + budType.getName());
        NPCEntity bud = null;
        for (NPCEntity currentBud : playerBudComponent.getCurrentBuds()) {
            if (currentBud.getNPCTypeId().equals(budType.getName())) {
                bud = currentBud;
                break;
            }
        }
        if (bud == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No bud of type " + budType.getName() + " found for player "
                            + playerBudComponent.getPlayerRef().getUuid());
            return;
        }
        Ref<EntityStore> budRef = bud.getReference();
        if (budRef == null || !budRef.isValid()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid reference for bud of type " + budType.getName() + " for player "
                            + playerBudComponent.getPlayerRef().getUuid());
            return;
        }
        BudComponent budComponent = store.getComponent(budRef, BudComponent.getComponentType());
        Set<BudType> budTypes = Set.of(budType);
        if (!budTypes.isEmpty()) {
            TeleportQueue.getInstance()
                    .addToCache(new TeleportEntry(playerBudComponent, budComponent, budTypes, store));
        }
    }

    private void teleportBud(TeleportEvent event, BudType budType) {
        PlayerBudComponent playerBudComponent = event.playerBudComponent();
        NPCEntity bud = playerBudComponent.getCurrentBuds().stream()
                .filter(b -> b.getNPCTypeId().equals(budType.getName()))
                .findFirst()
                .orElse(null);
        if (bud == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No bud of type " + budType.getName() + " found for player "
                            + playerBudComponent.getPlayerRef().getUuid());
            return;
        }
        Ref<EntityStore> budRef = bud.getReference();
        if (budRef == null || !budRef.isValid()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid reference for bud of type " + budType.getName() + " for player "
                            + playerBudComponent.getPlayerRef().getUuid());
            return;
        }

        PlayerRef playerRef = playerBudComponent.getPlayerRef();
        Store<EntityStore> store = event.store();

        ComponentType<EntityStore, TransformComponent> transformComponentType = TransformComponent.getComponentType();
        if (transformComponentType == null) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] TransformComponent type not found for teleportation.");
            return;
        }
        TransformComponent transform = store.getComponent(budRef, transformComponentType);
        if (transform == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Transform component not found for bud of type " + budType.getName()
                            + " for player "
                            + playerBudComponent.getPlayerRef().getUuid());
            return;
        }

        Vector3d targetPos = BudManager.getInstance().getPlayerPositionWithOffset(playerRef);
        store.getExternalData().getWorld().execute(() -> {
            bud.moveTo(budRef, targetPos.getX(), targetPos.getY(), targetPos.getZ(), store);
            store.addComponent(budRef, Teleport.getComponentType(),
                    Teleport.createExact(targetPos, transform.getRotation()));
        });

        // Force client to re-receive all entities after teleport
        Ref<EntityStore> viewerRef = playerRef.getReference();
        if (viewerRef != null && viewerRef.isValid()) {
            EntityTrackerSystems.despawnAll(viewerRef, store);
        }
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Teleported bud of type " + budType.getName() + " for player "
                        + playerBudComponent.getPlayerRef().getUsername());
    }

}
