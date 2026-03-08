package com.bud.feature.block;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record BlockEntry(@Nonnull String blockName, @Nonnull BlockInteraction interaction,
        @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return blockName;
    }

}
