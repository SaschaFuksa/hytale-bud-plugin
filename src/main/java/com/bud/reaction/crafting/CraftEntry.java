package com.bud.reaction.crafting;

import com.bud.reaction.ICacheEntry;

/**
 * Represents a crafting or bench-usage event entry.
 * 
 * @param itemId      The crafted item ID or bench display name
 * @param interaction The type of interaction (CRAFTED or USED)
 */
public record CraftEntry(String itemId, CraftInteraction interaction) implements ICacheEntry {
    @Override
    public String getName() {
        return itemId;
    }
}
