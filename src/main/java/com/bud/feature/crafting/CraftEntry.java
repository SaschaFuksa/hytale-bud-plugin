package com.bud.feature.crafting;

import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.feature.queue.IQueueEntry;

/**
 * Represents a crafting or bench-usage event entry.
 * 
 * @param itemId      The crafted item ID or bench display name
 * @param interaction The type of interaction (CRAFTED or USED)
 */
public record CraftEntry(String itemId, CraftInteraction interaction) implements IQueueEntry {

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPriority'");
    }

    @Override
    public LLMInteractionEntry getInteractionEntry() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInteractionEntry'");
    }
}
