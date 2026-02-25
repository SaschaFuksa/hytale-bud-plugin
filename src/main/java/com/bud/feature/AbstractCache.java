package com.bud.feature;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.bud.core.config.OrchestratorConfig;
import com.bud.feature.queue.IQueueEntry;

public abstract class AbstractCache {

    protected final Map<UUID, LinkedList<IQueueEntry>> cache = new ConcurrentHashMap<>();
    protected static final int MAX_HISTORY = 3;

    /**
     * Per-player timestamp of last enqueue call.
     * Used to throttle enqueue frequency so rapid-fire events
     * (e.g. placing 5 blocks in 2 seconds) don't each produce
     * a separate orchestrator event.
     */
    private final Map<UUID, Long> lastEnqueueTime = new ConcurrentHashMap<>();
    private final long enqueueCooldownMs = OrchestratorConfig.getInstance().getOrchestratorChannelCooldownMs();

    public LinkedList<IQueueEntry> getHistory(UUID playerId) {
        return new LinkedList<>(cache.getOrDefault(playerId, new LinkedList<>()));
    }

    public IQueueEntry pollHistory(UUID playerId) {
        final IQueueEntry[] result = new IQueueEntry[1];
        cache.computeIfPresent(playerId, (id, list) -> {
            if (!list.isEmpty()) {
                result[0] = list.removeFirst();
            }
            return list.isEmpty() ? null : list;
        });
        return result[0];
    }

    /**
     * Returns {@code true} if enough time has passed since the last enqueue
     * for this player, and updates the timestamp. The cooldown matches the
     * orchestrator's channel cooldown so we never enqueue faster than the
     * orchestrator would dispatch.
     */
    protected boolean shouldEnqueue(UUID playerId) {
        long now = System.currentTimeMillis();
        long last = lastEnqueueTime.getOrDefault(playerId, 0L);
        if (now - last >= enqueueCooldownMs) {
            lastEnqueueTime.put(playerId, now);
            return true;
        }
        return false;
    }

    public abstract void add(UUID playerId, IQueueEntry entry);
}
