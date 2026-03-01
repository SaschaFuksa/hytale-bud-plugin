package com.bud.feature.item;

import java.util.LinkedList;

import com.bud.feature.AbstractCache;
import com.bud.feature.LLMContextFactory;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentItemCache extends AbstractCache {

    private static final RecentItemCache INSTANCE = new RecentItemCache();

    private RecentItemCache() {
    }

    public static RecentItemCache getInstance() {
        return INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void add(String playerName, IQueueEntry entry) {
        if (!(entry instanceof ItemEntry itemEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentItemCache: " + entry);
            return;
        }
        cache.compute(playerName, (key, list) -> {

            LinkedList<ItemEntry> currentList = (list == null) ? new LinkedList<>()
                    : (LinkedList<ItemEntry>) (LinkedList<?>) list;

            for (IQueueEntry existingEntry : currentList) {
                if (existingEntry.getEntryName().equals(itemEntry.getEntryName())) {
                    return (LinkedList<IQueueEntry>) (LinkedList<?>) currentList;
                }
            }

            currentList.add(itemEntry);

            currentList.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

            while (currentList.size() > MAX_HISTORY) {
                currentList.removeLast();
            }

            LoggerUtil.getLogger()
                    .fine(() -> "[BUD-Cache] Player " + playerName + " picked up item: " + itemEntry.itemName());
            return (LinkedList<IQueueEntry>) (LinkedList<?>) currentList;
        });

        if (shouldEnqueue(playerName)) {
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.ACTIVITY,
                    entry,
                    "item",
                    playerName,
                    new LLMInteractionEntry(LLMItemMessageCreation.getInstance(),
                            LLMContextFactory.createContext(entry),
                            entry.getBudComponent()),
                    System.currentTimeMillis()));
        }
    }
}
