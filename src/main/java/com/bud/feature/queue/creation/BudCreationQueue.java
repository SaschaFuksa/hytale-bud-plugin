package com.bud.feature.queue.creation;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.feature.AbstractTracker;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class BudCreationQueue extends AbstractTracker {

    private static final BudCreationQueue INSTANCE = new BudCreationQueue();

    private final ConcurrentLinkedQueue<BudCreationEntry> cache = new ConcurrentLinkedQueue<>();

    private BudCreationQueue() {
    }

    public static BudCreationQueue getInstance() {
        return INSTANCE;
    }

    @Override
    public void startPolling() {
        if (isPolling()) {
            return;
        }
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::pollAndHandle), 500L, 500L,
                TimeUnit.MILLISECONDS));
        LoggerUtil.getLogger().fine(() -> "[BUD] Started bud creation polling task");
    }

    public void addToCache(@Nonnull BudCreationEntry entry) {
        cache.add(entry);
        if (!isPolling()) {
            startPolling();
        }
    }

    private void pollAndHandle() {
        BudCreationEntry entry = cache.poll();
        if (entry == null) {
            stopPolling();
            return;
        }
        try {
            handleBudCreation(entry);
        } catch (Exception e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Error handling bud creation: " + e.getMessage());
        } finally {
            if (cache.isEmpty()) {
                stopPolling();
            }
        }
    }

    private void handleBudCreation(@Nonnull BudCreationEntry entry) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Handling bud creation: " + entry);
        BudCreationEvent.dispatch(entry.playerRef(), entry.budTypes());
    }
}
