package com.bud.system;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentOpponentCache {

    public enum CombatState {
        ATTACKED,
        WAS_ATTACKED
    }

    public record OpponentEntry(String roleName, CombatState state) {
    }

    private static final Map<UUID, LinkedList<OpponentEntry>> cache = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 3;

    /**
     * Adds an opponent to the cache or updates the status.
     * 
     * @param playerId Player UUID
     * @param roleName NPC name (roleName)
     * @param state    Interaction status
     */
    public static void addOpponent(UUID playerId, String roleName, CombatState state) {
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            if (!list.isEmpty()) {
                OpponentEntry lastEntry = list.getLast();

                if (lastEntry.roleName().equals(roleName)) {
                    if (state == CombatState.ATTACKED && lastEntry.state() == CombatState.WAS_ATTACKED) {
                        list.removeLast();
                        list.addLast(new OpponentEntry(roleName, state));
                        LoggerUtil.getLogger().fine(() ->
                                "[BUD-Cache] Updated interaction with " + roleName + " to ATTACKED for " + playerId);
                    }
                    return list;
                }
            }

            list.addLast(new OpponentEntry(roleName, state));

            final int size = list.size();
            // Limit size
            if (size > MAX_HISTORY) {
                list.removeFirst();
            }

            // Debug output
            LoggerUtil.getLogger().fine(() ->
                    "[BUD-Cache] Added " + roleName + " (" + state + ") for " + playerId + ". History: " + size);
            return list;
        });
    }

    public static LinkedList<OpponentEntry> getHistory(UUID playerId) {
        // Return a copy to avoid external modification of the internal list without
        // using setHistory
        return new LinkedList<>(cache.getOrDefault(playerId, new LinkedList<>()));
    }

    /**
     * Retrieves and removes the oldest entry from the history.
     * Thread-safe operation.
     * 
     * @param playerId Player UUID
     * @return The oldest OpponentEntry or null if history is empty
     */
    public static OpponentEntry pollHistory(UUID playerId) {
        final OpponentEntry[] result = new OpponentEntry[1];
        cache.computeIfPresent(playerId, (id, list) -> {
            if (!list.isEmpty()) {
                result[0] = list.removeFirst();
            }
            return list.isEmpty() ? null : list;
        });
        return result[0];
    }

    public static void setHistory(UUID playerId, LinkedList<OpponentEntry> newHistory) {
        if (newHistory == null) {
            cache.remove(playerId);
        } else {
            // Ensure caching limit is respected even when setting externally
            while (newHistory.size() > MAX_HISTORY) {
                newHistory.removeFirst();
            }
            cache.put(playerId, newHistory);
        }
    }

    public static void clear(UUID playerId) {
        cache.remove(playerId);
    }
}
