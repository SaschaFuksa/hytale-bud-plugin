package com.bud.feature.bud;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record FavoriteDayEntry(@Nonnull BudComponent budComponent) implements IQueueEntry {

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return "favoriteDay";
    }
}
