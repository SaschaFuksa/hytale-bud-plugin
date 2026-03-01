package com.bud.feature;

import javax.annotation.Nonnull;

import com.bud.feature.block.BlockEntry;
import com.bud.feature.block.LLMBlockContext;
import com.bud.feature.combat.LLMCombatContext;
import com.bud.feature.combat.OpponentEntry;
import com.bud.feature.crafting.CraftEntry;
import com.bud.feature.crafting.LLMCraftContext;
import com.bud.feature.discover.DiscoverEntry;
import com.bud.feature.discover.LLMDiscoverContext;
import com.bud.feature.item.ItemEntry;
import com.bud.feature.item.LLMItemContext;
import com.bud.feature.queue.IQueueEntry;
import com.bud.llm.prompt.IPromptContext;

public class LLMContextFactory {

    @Nonnull
    public static IPromptContext createContext(IQueueEntry entry) {
        switch (entry) {
            case null -> throw new IllegalArgumentException("Entry cannot be null");
            case BlockEntry blockEntry -> {
                return LLMBlockContext.from(blockEntry);

            }
            case OpponentEntry opponentEntry -> {
                return LLMCombatContext.from(opponentEntry);
            }
            case CraftEntry craftEntry -> {
                return LLMCraftContext.from(craftEntry);
            }
            case DiscoverEntry discoverEntry -> {
                return LLMDiscoverContext.from(discoverEntry);
            }
            case ItemEntry itemEntry -> {
                return LLMItemContext.from(itemEntry);
            }
            default -> {
                throw new IllegalArgumentException("Unsupported entry type: " + entry.getClass().getName());
            }
        }
    }

}
