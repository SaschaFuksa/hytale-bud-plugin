package com.bud.scheduling;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.events.StateChangeEvent;
import com.bud.llm.message.state.LLMStateContext;
import com.bud.reaction.state.BudState;
import com.bud.reaction.tracker.AbstractTracker;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class StateChangeCache extends AbstractTracker {

    private static final StateChangeCache INSTANCE = new StateChangeCache();

    private final ConcurrentLinkedQueue<StateChangeEntry> cache = new ConcurrentLinkedQueue<>();

    private StateChangeCache() {
    }

    public static StateChangeCache getInstance() {
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
            handleState(entry);
        } catch (Exception e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Error handling state change: " + e.getMessage());
        } finally {
            if (cache.isEmpty()) {
                stopPolling();
            }
        }
    }

    private void handleState(@Nonnull StateChangeEntry entry) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Handling state change: " + entry);
        StateChangeEvent.dispatch(entry.bud(), entry.owner(), entry.newState());
        processInteraction(entry);
    }

    private void processInteraction(@Nonnull StateChangeEntry entry) {
        LLMStateContext context = new LLMStateContext(entry.newState().getStateName());
        LoggerUtil.getLogger()
                .severe(() -> "[BUD] Processing interaction for state: " + entry.newState().getStateName());
    }

    public record StateChangeEntry(@Nonnull NPCEntity bud, @Nonnull PlayerRef owner, @Nonnull BudState newState) {

    }

}
