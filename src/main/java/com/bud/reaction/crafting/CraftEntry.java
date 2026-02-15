package com.bud.reaction.crafting;

import com.bud.reaction.ICacheEntry;

/**
 * Represents a crafted recipe event entry.
 * 
 * @param itemId The crafted item ID (e.g. "Tool_Hatchet_Crude")
 */
public record CraftEntry(String itemId) implements ICacheEntry {
    @Override
    public String getName() {
        return itemId;
    }
}
