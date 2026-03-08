package com.bud.feature.combat;

import java.util.LinkedList;

import com.bud.feature.AbstractCache;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentOpponentCache extends AbstractCache {

    private static final RecentOpponentCache INSTANCE = new RecentOpponentCache();

    private RecentOpponentCache() {
    }

    public static RecentOpponentCache getInstance() {
        return INSTANCE;
    }

    @Override
    public void add(String playerName, IQueueEntry entry) {
        if (!(entry instanceof OpponentEntry opponentEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentOpponentCache: " + entry);
            return;
        }
        cache.compute(playerName, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            if (!list.isEmpty()) {
                OpponentEntry lastEntry = (OpponentEntry) list.getLast();

                if (lastEntry.getEntryName().equals(opponentEntry.getEntryName())) {
                    if (opponentEntry.isAttacked() && lastEntry.wasAttacked()) {
                        list.removeLast();
                        list.addLast(opponentEntry);
                        LoggerUtil.getLogger()
                                .fine(() -> "[BUD-Cache] Updated interaction with " + opponentEntry.getEntryName()
                                        + " to ATTACKED for " + playerName);
                    }
                    return list;
                }
            }

            list.addLast(opponentEntry);

            final int size = list.size();
            if (size > MAX_HISTORY) {
                list.removeFirst();
            }
            LoggerUtil.getLogger().fine(
                    () -> "[BUD-Cache] Added " + opponentEntry.getEntryName() + " (" + opponentEntry.state() + ") for "
                            + playerName + ". History: " + size);
            return list;
        });

        if (shouldEnqueue(playerName)) {
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.COMBAT,
                    entry,
                    "combat",
                    playerName,
                    new LLMInteractionEntry(LLMCombatMessageCreation.getInstance(),
                            opponentEntry),
                    System.currentTimeMillis()));
        }
    }

}
