package com.bud.feature;

import java.util.concurrent.ScheduledFuture;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public abstract class AbstractTracker {

    private volatile ScheduledFuture<?> pollingTask;

    public abstract void startPolling();

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
