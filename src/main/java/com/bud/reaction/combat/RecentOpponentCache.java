package com.bud.reaction.combat;

import java.util.LinkedList;
import java.util.UUID;

import com.bud.llm.message.combat.LLMCombatManager;
import com.bud.orchestrator.MessageChannel;
import com.bud.orchestrator.MessageOrchestrator;
import com.bud.orchestrator.QueuedEvent;
import com.bud.reaction.AbstractCache;
import com.bud.reaction.ICacheEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class RecentOpponentCache extends AbstractCache {

    private static final RecentOpponentCache INSTANCE = new RecentOpponentCache();

    private RecentOpponentCache() {
    }

    public static RecentOpponentCache getInstance() {
        return INSTANCE;
    }

    /**
     * Adds an opponent to the cache or updates the status.
     * 
     * @param playerId Player UUID
     * @param entry    OpponentEntry containing opponent information
     */
    @Override
    public void add(UUID playerId, ICacheEntry entry) {
        if (!(entry instanceof OpponentEntry opponentEntry)) {
            LoggerUtil.getLogger().severe(() -> "[BUD-Cache] Invalid entry type for RecentOpponentCache: " + entry);
            return;
        }
        cache.compute(playerId, (key, list) -> {
            if (list == null) {
                list = new LinkedList<>();
            }

            if (!list.isEmpty()) {
                OpponentEntry lastEntry = (OpponentEntry) list.getLast();

                if (lastEntry.getName().equals(opponentEntry.getName())) {
                    if (opponentEntry.isAttacked() && lastEntry.isWasAttacked()) {
                        list.removeLast();
                        list.addLast(opponentEntry);
                        LoggerUtil.getLogger()
                                .fine(() -> "[BUD-Cache] Updated interaction with " + opponentEntry.getName()
                                        + " to ATTACKED for " + playerId);
                    }
                    return list;
                }
            }

            list.addLast(opponentEntry);

            final int size = list.size();
            // Limit size
            if (size > MAX_HISTORY) {
                list.removeFirst();
            }

            // Debug output
            LoggerUtil.getLogger().fine(
                    () -> "[BUD-Cache] Added " + opponentEntry.getName() + " (" + opponentEntry.state() + ") for "
                            + playerId + ". History: " + size);
            return list;
        });

        // Enqueue to orchestrator (throttled to channel cooldown)
        if (shouldEnqueue(playerId)) {
            MessageOrchestrator.getInstance().enqueue(new QueuedEvent(
                    MessageChannel.COMBAT, 1, "combat",
                    new LLMCombatManager(), playerId, System.currentTimeMillis()));
        }
    }

}
