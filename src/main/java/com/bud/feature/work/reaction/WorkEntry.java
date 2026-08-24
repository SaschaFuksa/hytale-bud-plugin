package com.bud.feature.work.reaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.components.BudComponent;
import com.bud.core.types.WorkType;
import com.bud.feature.queue.IQueueEntry;

public record WorkEntry(@Nonnull BudComponent budComponent, @Nonnull WorkReactionKind kind,
        @Nullable WorkType workType, @Nullable String blockedItemId) implements IQueueEntry {

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    public int getPriority() {
        return kind.getPriority();
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return "work-" + kind.name();
    }

    @Override
    public boolean allowsReactionWhileWorking() {
        return true;
    }
}
