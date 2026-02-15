package com.bud.reaction.item;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.reaction.BaseCache;
import com.bud.reaction.ICacheEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Cache for recently picked items by players.
 * Used to provide context for Bud interactions.
 */
public class RecentItemCache extends BaseCache {

    private static final RecentItemCache INSTANCE = new RecentItemCache();

    private RecentItemCache() {
    }

    public static RecentItemCache getInstance() {
        return INSTANCE;
    }

    /**
     * Adds an item to the cache.
     * 
     * @param playerId Player UUID
     * @param entry    ItemEntry containing item information
     */
    @SuppressWarnings("unchecked")
    @Override
    public void add(UUID playerId, ICacheEntry entry) {
        if (!(entry instanceof ItemEntry itemEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentItemCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {

            LinkedList<ItemEntry> currentList = (list == null) ? new LinkedList<>()
                    : (LinkedList<ItemEntry>) (LinkedList<?>) list;

            // If item is already tracked, no need to add it again
            for (ICacheEntry existingEntry : currentList) {
                if (existingEntry.getName().equals(itemEntry.getName())) {
                    return (LinkedList<ICacheEntry>) (LinkedList<?>) currentList;
                }
            }

            currentList.add(itemEntry);

            // Sort by priority descending (highest priority at the start)
            currentList.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

            // Keep only the top items with highest priority
            while (currentList.size() > MAX_HISTORY) {
                currentList.removeLast();
            }

            LoggerUtil.getLogger()
                    .fine(() -> "[BUD-Cache] Player " + playerId + " picked up item: " + itemEntry.itemName());
            return (LinkedList<ICacheEntry>) (LinkedList<?>) currentList;
        });
    }
}
