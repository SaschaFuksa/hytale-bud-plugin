package com.bud.feature.queue.orchestrator;

import com.bud.feature.queue.IQueueEntry;
import com.bud.llm.interaction.LLMInteractionEntry;

public record OrchestratorQueue(
        OrchestratorChannel channel,
        IQueueEntry entry,
        String eventType,
        String playerName,
        LLMInteractionEntry interactionEntry,
        long timestamp) implements Comparable<OrchestratorQueue> {

    @Override
    public int compareTo(OrchestratorQueue other) {
        int cmp = Integer.compare(this.entry.getPriority(), other.entry.getPriority());
        if (cmp != 0) {
            return cmp;
        }
        return Long.compare(this.timestamp, other.timestamp);
    }
}
