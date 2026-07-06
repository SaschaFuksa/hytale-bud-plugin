package com.bud.feature.bud.reaction;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record BudReactionEntry(
        @Nonnull BudComponent budComponent,
        @Nonnull BudReactionKind kind,
        @Nonnull String situationInfo) implements IQueueEntry {

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return this.budComponent;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return "budReaction-" + this.kind.name();
    }
}
