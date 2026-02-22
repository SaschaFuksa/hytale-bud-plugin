package com.bud.llm.orchestrator;

import java.util.UUID;

import com.bud.llm.ILLMChatManager;

/**
 * Represents a queued event waiting to be dispatched to the LLM.
 * Events are prioritized within their channel (lower number = higher priority).
 *
 * @param channel     The message channel this event belongs to
 * @param priority    Priority within the channel (1 = highest)
 * @param eventType   Human-readable event type for logging (e.g. "discover",
 *                    "craft", "weather")
 * @param chatManager The LLM chat manager that will generate the prompt
 * @param playerId    The player who triggered the event
 * @param timestamp   When the event was enqueued (System.currentTimeMillis)
 */
public record QueuedEvent(
        MessageChannel channel,
        int priority,
        String eventType,
        ILLMChatManager chatManager,
        UUID playerId,
        long timestamp) implements Comparable<QueuedEvent> {

    /**
     * Compares by priority (ascending = higher priority first).
     * On equal priority, earlier timestamp wins.
     */
    @Override
    public int compareTo(QueuedEvent other) {
        int cmp = Integer.compare(this.priority, other.priority);
        if (cmp != 0) {
            return cmp;
        }
        return Long.compare(this.timestamp, other.timestamp);
    }
}
