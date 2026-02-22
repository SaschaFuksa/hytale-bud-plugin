package com.bud.reaction.item;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.llm.messages.item.LLMItemManager;
import com.bud.queue.IQueueEntry;
import com.bud.queue.orchestrator.OrchestratorChannel;
import com.bud.queue.orchestrator.Orchestrator;
import com.bud.queue.orchestrator.OrchestratorQueue;
import com.bud.reaction.AbstractCache;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Cache for recently picked items by players.
 * Used to provide context for Bud interactions.
 */
public class RecentItemCache extends AbstractCache {

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
    public void add(UUID playerId, IQueueEntry entry) {
        if (!(entry instanceof ItemEntry itemEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentItemCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {

            LinkedList<ItemEntry> currentList = (list == null) ? new LinkedList<>()
                    : (LinkedList<ItemEntry>) (LinkedList<?>) list;

            // If item is already tracked, no need to add it again
            for (IQueueEntry existingEntry : currentList) {
                if (existingEntry.getName().equals(itemEntry.getName())) {
                    return (LinkedList<IQueueEntry>) (LinkedList<?>) currentList;
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
            return (LinkedList<IQueueEntry>) (LinkedList<?>) currentList;
        });

        // Enqueue to orchestrator (throttled to channel cooldown)
        if (shouldEnqueue(playerId)) {
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.ACTIVITY, 3, "item",
                    new LLMItemManager(), playerId, System.currentTimeMillis()));
        }
    }
}
