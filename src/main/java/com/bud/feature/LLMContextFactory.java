package com.bud.feature;

import javax.annotation.Nonnull;

import com.bud.feature.block.BlockEntry;
import com.bud.feature.block.LLMBlockContext;
import com.bud.feature.combat.LLMCombatContext;
import com.bud.feature.combat.OpponentEntry;
import com.bud.feature.queue.IQueueEntry;
import com.bud.llm.prompt.IPromptContext;

public class LLMContextFactory {

    @Nonnull
    public static IPromptContext createContext(IQueueEntry entry) {
        switch (entry) {
            case null -> throw new IllegalArgumentException("Entry cannot be null");
            case BlockEntry blockEntry -> {
                return LLMBlockContext.from(blockEntry.getEntryName(), blockEntry.interaction(),
                        blockEntry.getBudComponent());

            }
            case OpponentEntry opponentEntry -> {
                return LLMCombatContext.from(opponentEntry,
                        opponentEntry.getBudComponent());
            }
            default -> {
                throw new IllegalArgumentException("Unsupported entry type: " + entry.getClass().getName());
            }
        }
    }

}
