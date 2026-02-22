package com.bud.queue.state;

import javax.annotation.Nonnull;

import com.bud.queue.IQueueEntry;
import com.bud.queue.InteractionEntry;
import com.bud.reaction.state.BudState;

public record StateChangeEntry(@Nonnull BudState newState, @Nonnull InteractionEntry interactionEntry)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public InteractionEntry getInteractionEntry() {
        return interactionEntry;
    }

}
