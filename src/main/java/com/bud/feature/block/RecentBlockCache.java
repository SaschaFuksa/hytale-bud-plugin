package com.bud.feature.block;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.feature.AbstractCache;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentBlockCache extends AbstractCache {

    private static final RecentBlockCache INSTANCE = new RecentBlockCache();

    private RecentBlockCache() {
    }

    public static RecentBlockCache getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(UUID playerId, IQueueEntry entry) {
        if (!(entry instanceof BlockEntry blockEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentBlockCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            if (!list.isEmpty() && list.getLast() instanceof BlockEntry lastEntry
                    && lastEntry.getEntryName().equals(blockEntry.getEntryName())
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

        if (shouldEnqueue(playerId)) {
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.ACTIVITY, 4, "block",
                    LLMBlockManager.getInstance(), playerId, System.currentTimeMillis()));
        }
    }
}
