package com.bud.reaction.item;

import com.bud.queue.IQueueEntry;

public record ItemEntry(String itemName, int priority, ItemInteraction interaction) implements IQueueEntry {

    @Override
    public String getName() {
        return itemName;
    }

}
