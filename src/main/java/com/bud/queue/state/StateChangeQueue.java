package com.bud.queue.state;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.events.StateChangeEvent;
import com.bud.interaction.InteractionManager;
import com.bud.llm.messages.state.LLMStateContext;
import com.bud.llm.messages.state.LLMStateMessageCreation;
import com.bud.profile.IBudProfile;
import com.bud.queue.AbstractQueue;
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
        BudComponent budComponent = entry.getInteractionEntry().budComponent();
        StateChangeEvent.dispatch(budComponent.getBud(), budComponent.getPlayerRef(), entry.newState());
        Ref<EntityStore> entityRef = budComponent.getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: " + budComponent.getBud());
            return;
        }
        IBudProfile budProfile = entry.getInteractionEntry().getBudProfile();
        LLMStateContext context = LLMStateContext.from(budComponent, budProfile);
        if (context == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to create LLMStateContext for Bud: " + budComponent.getBud());
            return;
        }
        InteractionManager.getInstance().processInteraction(LLMStateMessageCreation.getInstance(), context,
                budComponent, budProfile);
    }

}
