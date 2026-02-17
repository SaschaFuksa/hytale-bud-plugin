package com.bud.reaction.item;

import com.bud.reaction.ICacheEntry;

public record ItemEntry(String itemName, int priority, ItemInteraction interaction) implements ICacheEntry {

    @Override
    public String getName() {
        return itemName;
    }

}
