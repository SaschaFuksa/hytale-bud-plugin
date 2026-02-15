package com.bud.reaction;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseCache {

    protected static final Map<UUID, LinkedList<ICacheEntry>> cache = new ConcurrentHashMap<>();
    protected static final int MAX_HISTORY = 3;

    public static LinkedList<ICacheEntry> getHistory(UUID playerId) {
        return new LinkedList<>(cache.getOrDefault(playerId, new LinkedList<>()));
    }

    public static ICacheEntry pollHistory(UUID playerId) {
        final ICacheEntry[] result = new ICacheEntry[1];
        cache.computeIfPresent(playerId, (id, list) -> {
            if (!list.isEmpty()) {
                result[0] = list.removeFirst();
            }
            return list.isEmpty() ? null : list;
        });
        return result[0];
    }

    public abstract void add(UUID playerId, ICacheEntry entry);
}
