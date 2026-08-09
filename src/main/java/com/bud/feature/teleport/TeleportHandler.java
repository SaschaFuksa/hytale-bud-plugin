package com.bud.feature.teleport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.feature.queue.teleport.TeleportEntry;
import com.bud.feature.queue.teleport.TeleportQueue;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TeleportHandler implements Consumer<TeleportEvent> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final long TELEPORT_DELAY_MS = 250;
    private static final long CHUNK_POLL_INTERVAL_MS = 100;
    private static final int CHUNK_POLL_MAX_ATTEMPTS = 30;

    @Override
    public void accept(TeleportEvent event) {
        SCHEDULER.schedule(() -> {
            event.store().getExternalData().getWorld().execute(() -> {
                this.awaitTargetChunkAndTeleport(event, 0);
            });
        }, TELEPORT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void awaitTargetChunkAndTeleport(TeleportEvent event, int attempt) {
        World world = event.store().getExternalData().getWorld();
        PlayerRef playerRef = event.budComponent().getPlayerRef();
        Vector3d targetPos = BudManager.getInstance().getSpawnPosition(playerRef, 0, 1);

        if (!isChunkLoaded(world, targetPos.x, targetPos.z) && attempt < CHUNK_POLL_MAX_ATTEMPTS) {
            SCHEDULER.schedule(
                    () -> world.execute(() -> this.awaitTargetChunkAndTeleport(event, attempt + 1)),
                    CHUNK_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            return;
        }
        if (attempt >= CHUNK_POLL_MAX_ATTEMPTS) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Target chunk for bud teleport still not loaded after "
                            + (CHUNK_POLL_MAX_ATTEMPTS * CHUNK_POLL_INTERVAL_MS) + "ms, teleporting anyway.");
        }
        this.teleportBud(event, targetPos);
    }

    private static boolean isChunkLoaded(@Nonnull World world, double x, double z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock((int) Math.floor(x), (int) Math.floor(z));
        return world.getChunkIfInMemory(chunkIndex) != null;
    }

    private void teleportBud(TeleportEvent event, Vector3d targetPos) {
        BudComponent budComponent = event.budComponent();
        Ref<EntityStore> budRef = budComponent.getBud().getReference();
        if (budRef == null || !budRef.isValid()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid reference for bud " + budComponent.getBud().getNPCTypeId()
                            + " for player "
                            + budComponent.getPlayerRef().getUsername());
            return;
        }

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
                    .warning(() -> "[BUD] Transform component not found for bud of type "
                            + budComponent.getBud().getNPCTypeId()
                            + " for player "
                            + budComponent.getPlayerRef().getUsername());
            return;
        }

        budComponent.getBud().moveTo(budRef, targetPos.x, targetPos.y, targetPos.z, store);
        store.addComponent(budRef, Teleport.getComponentType(),
                Teleport.createExact(targetPos, transform.getRotation()));
        if (event.shouldSendReaction()) {
            TeleportQueue.getInstance()
                    .addToCache(new TeleportEntry(budComponent, store));
        }
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Teleported bud of type " + budComponent.getBud().getNPCTypeId() + " for player "
                        + budComponent.getPlayerRef().getUsername());
    }

}
