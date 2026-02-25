package com.bud.feature.queue.teleport;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.interaction.LLMInteractionManager;
import com.bud.feature.queue.AbstractQueue;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TeleportQueue extends AbstractQueue {

    private static final TeleportQueue INSTANCE = new TeleportQueue();

    private TeleportQueue() {
    }

    public static TeleportQueue getInstance() {
        return INSTANCE;
    }

    @Override
    protected void pollAndHandle() {
        TeleportEntry entry = (TeleportEntry) cache.poll();
        if (entry == null) {
            stopPolling();
            return;
        }
        try {
            handleTeleport(entry);
        } catch (Exception e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Error handling teleport: " + e.getMessage());
        } finally {
            if (cache.isEmpty()) {
                stopPolling();
            }
        }
    }

    @SuppressWarnings("null")
    private void handleTeleport(@Nonnull TeleportEntry entry) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Handling teleport: " + entry.budTypes());
        BudComponent budComponent = entry.getInteractionEntry().budComponent();
        TeleportEvent.dispatch(entry.store(), entry.playerBudComponent(), entry.budTypes());

        Ref<EntityStore> entityRef = budComponent.getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: " + budComponent.getBud());
            return;
        }
        LLMTeleportContext context = LLMTeleportContext.from(budComponent, budProfile);
        if (context == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to create LLMTeleportContext for Bud: " + budComponent.getBud());
            return;
        }
        LLMInteractionManager.getInstance().processInteraction(
                new LLMInteractionEntry(LLMTeleportMessageCreation.getInstance(), context, budComponent));
    }

}
