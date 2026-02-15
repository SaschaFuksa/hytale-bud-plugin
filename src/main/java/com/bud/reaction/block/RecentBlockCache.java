package com.bud.reaction.block;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.llm.message.block.LLMBlockManager;
import com.bud.orchestrator.MessageChannel;
import com.bud.orchestrator.MessageOrchestrator;
import com.bud.orchestrator.QueuedEvent;
import com.bud.reaction.BaseCache;
import com.bud.reaction.ICacheEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Cache for recently broken or added blocks by players.
 * Used to provide context for Bud interactions.
 */
public class RecentBlockCache extends BaseCache {

    private static final RecentBlockCache INSTANCE = new RecentBlockCache();

    private RecentBlockCache() {
    }

    public static RecentBlockCache getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a block interaction to the cache.
     * 
     * @param playerId Player UUID
     * @param entry    BlockEntry containing block information
     */
    @Override
    public void add(UUID playerId, ICacheEntry entry) {
        if (!(entry instanceof BlockEntry blockEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentBlockCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            // Avoid duplicate consecutive entries of the same block type
            if (!list.isEmpty() && list.getLast() instanceof BlockEntry lastEntry
                    && lastEntry.getName().equals(blockEntry.getName())
                    && lastEntry.interaction() == blockEntry.interaction()) {
                return list;
            }

            list.addLast(blockEntry);

            if (list.size() > MAX_HISTORY) {
                list.removeFirst();
            }

            LoggerUtil.getLogger()
                    .fine(() -> "[BUD-Cache] Player " + playerId + " " + blockEntry.interaction() + " block: "
                            + blockEntry.getName());
            return list;
        });

        // Enqueue to orchestrator (throttled to channel cooldown)
        if (shouldEnqueue(playerId)) {
            MessageOrchestrator.getInstance().enqueue(new QueuedEvent(
                    MessageChannel.ACTIVITY, 4, "block",
                    LLMBlockManager.getInstance(), playerId, System.currentTimeMillis()));
        }
    }
}
