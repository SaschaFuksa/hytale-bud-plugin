package com.bud.feature.queue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public abstract class AbstractQueue {

    private final long INITIAL_DELAY_MS = 250L;
    private final long POLLING_INTERVAL_MS = 250L;

    private volatile ScheduledFuture<?> pollingTask;

    protected final ConcurrentLinkedQueue<IQueueEntry> cache = new ConcurrentLinkedQueue<>();

    public void addToCache(@Nonnull IQueueEntry entry) {
        cache.add(entry);
        if (!isPolling()) {
            startPolling();
        }
    }

    protected void startPolling() {
        if (isPolling()) {
            return;
        }
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::pollAndHandle), INITIAL_DELAY_MS, POLLING_INTERVAL_MS,
                TimeUnit.MILLISECONDS));
        LoggerUtil.getLogger().fine(() -> "[BUD] Started " + this.getClass().getSimpleName() + " polling task");
    }

    protected abstract void pollAndHandle();

    protected boolean isPolling() {
        return pollingTask != null && !pollingTask.isCancelled();
    }

    protected void setPollingTask(ScheduledFuture<?> task) {
        this.pollingTask = task;
    }

    public synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
            LoggerUtil.getLogger().fine(() -> "[BUD] Stopped " + this.getClass().getSimpleName() + " polling task");
        }
    }
}
