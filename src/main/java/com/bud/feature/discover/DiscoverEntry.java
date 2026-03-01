package com.bud.feature.discover;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record DiscoverEntry(@Nonnull String zoneName, @Nonnull String regionName, boolean major,
        @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 5;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return zoneName;
    }
}
