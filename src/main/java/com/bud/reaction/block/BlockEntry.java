package com.bud.reaction.block;

import com.bud.queue.IQueueEntry;

public record BlockEntry(String blockName, BlockInteraction interaction) implements IQueueEntry {

    @Override
    public String getName() {
        return blockName;
    }

}
