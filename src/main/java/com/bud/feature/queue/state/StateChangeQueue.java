package com.bud.feature.queue.state;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMInteractionManager;
import com.bud.feature.queue.AbstractQueue;
import com.bud.feature.state.LLMStateMessageCreation;
import com.bud.feature.state.StateChangeEvent;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class StateChangeQueue extends AbstractQueue {

    private static final StateChangeQueue INSTANCE = new StateChangeQueue();

    private StateChangeQueue() {
    }

    public static StateChangeQueue getInstance() {
        return INSTANCE;
    }

    @Override
    protected void pollAndHandle() {
        StateChangeEntry entry = (StateChangeEntry) cache.poll();
        if (entry == null) {
            stopPolling();
            return;
        }
        try {
            handleStateChange(entry);
        } catch (Exception e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Error handling state change: " + e.getMessage());
        } finally {
            if (cache.isEmpty()) {
                stopPolling();
            }
        }
    }

    private void handleStateChange(@Nonnull StateChangeEntry entry) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Handling state change: " + entry);
        BudComponent budComponent = entry.getBudComponent();
        budComponent.setCurrentState(entry.newState());
        StateChangeEvent.dispatch(budComponent.getBud(), budComponent.getPlayerRef(), entry.newState());
        Ref<EntityStore> entityRef = budComponent.getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: " + budComponent.getBud());
            return;
        }
        LLMInteractionManager.getInstance().processInteraction(
                new LLMInteractionEntry(LLMStateMessageCreation.getInstance(), entry));
    }

}
