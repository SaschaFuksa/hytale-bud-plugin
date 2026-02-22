package com.bud.queue.state;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.queue.ICacheEntry;
import com.bud.reaction.state.BudState;

public record StateChangeEntry(@Nonnull BudComponent budComponent, @Nonnull BudState newState) implements ICacheEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

}
