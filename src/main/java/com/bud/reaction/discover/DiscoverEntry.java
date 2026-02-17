package com.bud.reaction.discover;

import com.bud.reaction.ICacheEntry;

/**
 * Represents a discovered zone event entry.
 * 
 * @param zoneName   The zone name (e.g. "Emerald_Wilds")
 * @param regionName The region name (e.g. "Zone1_Shore")
 * @param major      Whether this is a major discovery
 */
public record DiscoverEntry(String zoneName, String regionName, boolean major) implements ICacheEntry {
    @Override
    public String getName() {
        return zoneName;
    }
}
