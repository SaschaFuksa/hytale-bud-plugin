package com.bud.feature.queue.state;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.types.BudState;
import com.bud.feature.queue.IQueueEntry;

public record StateChangeEntry(@Nonnull BudState newState, @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    public String getStateInformation() {
        return "The current state of you is: " + newState.getStateName();
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return newState.getStateName();
    }

}
