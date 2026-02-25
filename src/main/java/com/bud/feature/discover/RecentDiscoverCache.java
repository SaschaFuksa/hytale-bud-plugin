package com.bud.feature.discover;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.feature.queue.IQueueEntry;
import com.bud.reaction.AbstractCache;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Cache for recently discovered zones by players.
 * Used to provide context for Bud interactions.
 */
public class RecentDiscoverCache extends AbstractCache {

    private static final RecentDiscoverCache INSTANCE = new RecentDiscoverCache();

    private RecentDiscoverCache() {
    }

    public static RecentDiscoverCache getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(UUID playerId, IQueueEntry entry) {
        if (!(entry instanceof DiscoverEntry discoverEntry)) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD-Cache] Invalid entry type for RecentDiscoverCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            // Deduplicate: don't add if last entry has the same zone name
            if (!list.isEmpty() && list.getLast() instanceof DiscoverEntry lastEntry
                    && lastEntry.zoneName().equals(discoverEntry.zoneName())
                    && lastEntry.regionName().equals(discoverEntry.regionName())) {
                return list;
            }

            list.addLast(discoverEntry);
            if (list.size() > MAX_HISTORY) {
                list.removeFirst();
            }

            LoggerUtil.getLogger()
                    .fine(() -> "[BUD-Cache] Player " + playerId + " discovered zone: "
                            + discoverEntry.zoneName() + " region: " + discoverEntry.regionName());
            return list;
        });

        // Enqueue to orchestrator (throttled to channel cooldown)
        // if (shouldEnqueue(playerId)) {
        // Orchestrator.getInstance().enqueue(new OrchestratorQueue(
        // OrchestratorChannel.AMBIENT, 1, "discover",
        // LLMDiscoverManager.getInstance(), playerId, System.currentTimeMillis()));
        // }
    }
}
