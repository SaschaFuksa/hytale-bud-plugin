package com.bud.feature.discover;

import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.feature.queue.IQueueEntry;

/**
 * Represents a discovered zone event entry.
 * 
 * @param zoneName   The zone name (e.g. "Emerald_Wilds")
 * @param regionName The region name (e.g. "Zone1_Shore")
 * @param major      Whether this is a major discovery
 */
public record DiscoverEntry(String zoneName, String regionName, boolean major) implements IQueueEntry {

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
