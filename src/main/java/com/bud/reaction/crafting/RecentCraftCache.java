package com.bud.reaction.crafting;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.llm.message.craft.LLMCraftManager;
import com.bud.orchestrator.MessageChannel;
import com.bud.orchestrator.MessageOrchestrator;
import com.bud.orchestrator.QueuedEvent;
import com.bud.reaction.BaseCache;
import com.bud.reaction.ICacheEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Cache for recently crafted items by players.
 * Used to provide context for Bud interactions.
 */
public class RecentCraftCache extends BaseCache {

    private static final RecentCraftCache INSTANCE = new RecentCraftCache();

    private RecentCraftCache() {
    }

    public static RecentCraftCache getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(UUID playerId, ICacheEntry entry) {
        if (!(entry instanceof CraftEntry craftEntry)) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD-Cache] Invalid entry type for RecentCraftCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            // Deduplicate: don't add if last entry has the same item ID and interaction
            // type
            if (!list.isEmpty() && list.getLast() instanceof CraftEntry lastEntry
                    && lastEntry.itemId().equals(craftEntry.itemId())
                    && lastEntry.interaction() == craftEntry.interaction()) {
                return list;
            }

            list.addLast(craftEntry);
            if (list.size() > MAX_HISTORY) {
                list.removeFirst();
            }

            LoggerUtil.getLogger()
                    .fine(() -> "[BUD-Cache] Player " + playerId + " " + craftEntry.interaction().name().toLowerCase()
                            + " item: " + craftEntry.itemId());
            return list;
        });

        // Enqueue to orchestrator (throttled to channel cooldown)
        if (shouldEnqueue(playerId)) {
            int priority = (craftEntry.interaction() == CraftInteraction.CRAFTED) ? 1 : 2;
            MessageOrchestrator.getInstance().enqueue(new QueuedEvent(
                    MessageChannel.ACTIVITY, priority, "craft",
                    LLMCraftManager.getInstance(), playerId, System.currentTimeMillis()));
        }
    }
}
