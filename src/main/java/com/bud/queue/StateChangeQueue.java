package com.bud.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.config.LLMConfig;
import com.bud.events.ChatEvent;
import com.bud.events.SoundEvent;
import com.bud.events.StateChangeEvent;
import com.bud.llm.LLMCaller;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.state.LLMStateContext;
import com.bud.llm.messages.state.LLMStateMessageCreation;
import com.bud.mappings.BudProfileMapper;
import com.bud.profile.IBudProfile;
import com.bud.reaction.tracker.AbstractTracker;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class StateChangeQueue extends AbstractTracker {

    private static final StateChangeQueue INSTANCE = new StateChangeQueue();

    private final ConcurrentLinkedQueue<StateChangeEntry> cache = new ConcurrentLinkedQueue<>();

    private StateChangeQueue() {
    }

    public static StateChangeQueue getInstance() {
        return INSTANCE;
    }

    public void addToCache(@Nonnull StateChangeEntry entry) {
        cache.add(entry);
        if (!isPolling()) {
            startPolling();
        }
    }

    @Override
    public void startPolling() {
        if (isPolling()) {
            return;
        }
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::pollAndHandle), 250L, 250L,
                TimeUnit.MILLISECONDS));
        LoggerUtil.getLogger().fine(() -> "[BUD] Started state polling task");
    }

    private void pollAndHandle() {
        StateChangeEntry entry = cache.poll();
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
        StateChangeEvent.dispatch(entry.budComponent().getBud(), entry.budComponent().getPlayerRef(), entry.newState());
        processInteraction(entry);
    }

    private void processInteraction(@Nonnull StateChangeEntry entry) {
        Ref<EntityStore> entityRef = entry.budComponent().getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: " + entry.budComponent().getBud());
            return;
        }
        IBudProfile budProfile = BudProfileMapper.getInstance().getProfileForBudType(entry.budComponent().getBudType());
        LLMStateContext context = LLMStateContext.from(entry.budComponent(), budProfile);
        Prompt prompt = LLMStateMessageCreation.getInstance().createPrompt(context);
        if (prompt == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] No prompt found for state: " + entry.newState().getStateName());
            return;
        }
        String message;
        if (LLMConfig.getInstance().isEnableLLM()) {
            message = LLMCaller.getInstance().callLLM(prompt, budProfile).join();
        } else {
            message = prompt.systemPrompt();
        }
        if (message == null || message.isBlank()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] LLM returned empty message for state: " + entry.newState().getStateName());
            return;
        }
        ChatEvent.dispatch(entry.budComponent().getPlayerRef(), message);
        SoundEvent.dispatch(entityRef, budProfile.getBudSoundData().getPassiveSound());
        LoggerUtil.getLogger()
                .severe(() -> "[BUD] Processing interaction for state: " + entry.newState().getStateName());
    }

}
