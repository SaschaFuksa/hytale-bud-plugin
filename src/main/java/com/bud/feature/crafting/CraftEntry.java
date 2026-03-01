package com.bud.feature.crafting;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record CraftEntry(@Nonnull String itemId, @Nonnull CraftInteraction interaction,
        @Nonnull BudComponent budComponent) implements IQueueEntry {

    @Override
    public int getPriority() {
        return (interaction == CraftInteraction.CRAFTED) ? 1 : 2;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return itemId;
    }
}
