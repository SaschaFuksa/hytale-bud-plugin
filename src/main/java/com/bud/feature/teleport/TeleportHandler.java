package com.bud.feature.teleport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.feature.queue.teleport.TeleportEntry;
import com.bud.feature.queue.teleport.TeleportQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TeleportHandler implements Consumer<TeleportEvent> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final long TELEPORT_DELAY_MS = 250;

    @Override
    public void accept(TeleportEvent event) {
        SCHEDULER.schedule(() -> {
            event.store().getExternalData().getWorld().execute(() -> {
                this.teleportBud(event);
            });
        }, TELEPORT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void teleportBud(TeleportEvent event) {
        BudComponent budComponent = event.budComponent();
        Ref<EntityStore> budRef = budComponent.getBud().getReference();
        if (budRef == null || !budRef.isValid()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid reference for bud " + budComponent.getBud().getNPCTypeId()
                            + " for player "
                            + budComponent.getPlayerRef().getUsername());
            return;
        }

        PlayerRef playerRef = budComponent.getPlayerRef();
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

        Vector3d targetPos = BudManager.getInstance().getPlayerPositionWithOffset(playerRef);
        store.getExternalData().getWorld().execute(() -> {
            budComponent.getBud().moveTo(budRef, targetPos.getX(), targetPos.getY(), targetPos.getZ(), store);
            store.addComponent(budRef, Teleport.getComponentType(),
                    Teleport.createExact(targetPos, transform.getRotation()));
        });
        LLMInteractionEntry interactionEntry = new LLMInteractionEntry(LLMTeleportMessageCreation.getInstance(),
                LLMTeleportContext.from(budComponent));
        TeleportQueue.getInstance()
                .addToCache(new TeleportEntry(budComponent, store, interactionEntry));
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Teleported bud of type " + budComponent.getBud().getNPCTypeId() + " for player "
                        + budComponent.getPlayerRef().getUsername());
    }

}
