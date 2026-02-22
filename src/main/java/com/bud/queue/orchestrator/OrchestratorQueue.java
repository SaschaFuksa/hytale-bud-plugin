package com.bud.queue.orchestrator;

import com.bud.queue.IQueueEntry;

public record OrchestratorQueue(
        OrchestratorChannel channel,
        String eventType,
        IQueueEntry cacheEntry,
        long timestamp) implements Comparable<OrchestratorQueue> {

    @Override
    public int compareTo(OrchestratorQueue other) {
        int cmp = Integer.compare(this.cacheEntry.getPriority(), other.cacheEntry.getPriority());
        if (cmp != 0) {
            return cmp;
        }
        return Long.compare(this.timestamp, other.timestamp);
    }
}
