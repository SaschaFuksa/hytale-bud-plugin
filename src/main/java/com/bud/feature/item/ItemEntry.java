package com.bud.feature.item;

import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.feature.queue.IQueueEntry;

public record ItemEntry(String itemName, int priority, ItemInteraction interaction) implements IQueueEntry {

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
