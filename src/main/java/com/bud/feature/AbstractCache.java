package com.bud.feature;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bud.core.config.OrchestratorConfig;
import com.bud.feature.queue.IQueueEntry;

public abstract class AbstractCache {

    protected final Map<String, LinkedList<IQueueEntry>> cache = new ConcurrentHashMap<>();

    protected static final int MAX_HISTORY = 3;

    private final Map<String, Long> lastEnqueueTime = new ConcurrentHashMap<>();

    private final long enqueueCooldownMs = OrchestratorConfig.getInstance().getOrchestratorChannelCooldownMs();

    protected boolean shouldEnqueue(String playerName) {
        long now = System.currentTimeMillis();
        long last = lastEnqueueTime.getOrDefault(playerName, 0L);
        if (now - last >= enqueueCooldownMs) {
            lastEnqueueTime.put(playerName, now);
            return true;
        }
        return false;
    }

    public void clearPlayer(String playerName) {
        cache.remove(playerName);
        lastEnqueueTime.remove(playerName);
    }

    public abstract void add(String playerName, IQueueEntry entry);
}
