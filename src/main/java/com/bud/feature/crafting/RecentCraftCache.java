package com.bud.feature.crafting;

import java.util.LinkedList;

import com.bud.feature.AbstractCache;
import com.bud.feature.LLMContextFactory;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentCraftCache extends AbstractCache {

    private static final RecentCraftCache INSTANCE = new RecentCraftCache();

    private RecentCraftCache() {
    }

    public static RecentCraftCache getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(String playerName, IQueueEntry entry) {
        if (!(entry instanceof CraftEntry craftEntry)) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD-Cache] Invalid entry type for RecentCraftCache: " + entry);
            return;
        }
        cache.compute(playerName, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

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
                    .fine(() -> "[BUD-Cache] Player " + playerName + " " + craftEntry.interaction().name().toLowerCase()
                            + " item: " + craftEntry.itemId());
            return list;
        });

        if (shouldEnqueue(playerName)) {
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.ACTIVITY,
                    entry,
                    "craft",
                    playerName,
                    new LLMInteractionEntry(LLMCraftMessageCreation.getInstance(),
                            LLMContextFactory.createContext(entry)),
                    System.currentTimeMillis()));
        }
    }
}
