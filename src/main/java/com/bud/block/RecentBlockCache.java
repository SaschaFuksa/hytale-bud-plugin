package com.bud.block;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Cache for recently broken blocks by players.
 * Used to provide context for Bud interactions.
 */
public class RecentBlockCache {

    public record BlockEntry(String blockName, BlockInteraction interaction) {
    }

    private static final Map<UUID, LinkedList<BlockEntry>> cache = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 3;

    /**
     * Adds a broken block to the cache.
     * 
     * @param playerId  Player UUID
     * @param blockName Name/ID of the block
     */
    public static void addBlock(UUID playerId, String blockName, BlockInteraction interaction) {
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            // Avoid duplicate consecutive entries of the same block type
            if (!list.isEmpty() && list.getLast().blockName().equals(blockName)
                    && list.getLast().interaction() == interaction) {
                return list;
            }

            list.addLast(new BlockEntry(blockName, interaction));

            if (list.size() > MAX_HISTORY) {
                list.removeFirst();
            }

            LoggerUtil.getLogger()
                    .fine(() -> "[BUD-Cache] Player " + playerId + " " + interaction + " block: " + blockName);
            return list;
        });

        // Trigger the block chat scheduler
        BlockChatScheduler.getInstance().onBlockEvent(playerId);
    }

    public static LinkedList<BlockEntry> getHistory(UUID playerId) {
        return new LinkedList<>(cache.getOrDefault(playerId, new LinkedList<>()));
    }

    public static BlockEntry pollHistory(UUID playerId) {
        final BlockEntry[] result = new BlockEntry[1];
        cache.computeIfPresent(playerId, (id, list) -> {
            if (!list.isEmpty()) {
                result[0] = list.removeFirst();
            }
            return list.isEmpty() ? null : list;
        });
        return result[0];
    }
}
