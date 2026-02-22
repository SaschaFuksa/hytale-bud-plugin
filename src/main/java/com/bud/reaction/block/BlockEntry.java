package com.bud.reaction.block;

import com.bud.queue.ICacheEntry;

public record BlockEntry(String blockName, BlockInteraction interaction) implements ICacheEntry {

    @Override
    public String getName() {
        return blockName;
    }

}
