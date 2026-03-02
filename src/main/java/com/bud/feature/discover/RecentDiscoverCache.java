package com.bud.feature.discover;

import java.util.LinkedList;

import com.bud.feature.AbstractCache;
import com.bud.feature.LLMContextFactory;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentDiscoverCache extends AbstractCache {

    private static final RecentDiscoverCache INSTANCE = new RecentDiscoverCache();

    private RecentDiscoverCache() {
    }

    public static RecentDiscoverCache getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(String playerName, IQueueEntry entry) {
        if (!(entry instanceof DiscoverEntry discoverEntry)) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD-Cache] Invalid entry type for RecentDiscoverCache: " + entry);
            return;
        }
        cache.compute(playerName, (key, list) -> {
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
                    .fine(() -> "[BUD-Cache] Player " + playerName + " discovered zone: "
                            + discoverEntry.zoneName() + " region: " + discoverEntry.regionName());
            return list;
        });

        if (shouldEnqueue(playerName)) {
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.AMBIENT,
                    entry,
                    "discover",
                    playerName,
                    new LLMInteractionEntry(LLMDiscoverMessageCreation.getInstance(),
                            LLMContextFactory.createContext(entry)),
                    System.currentTimeMillis()));
        }
    }
}
