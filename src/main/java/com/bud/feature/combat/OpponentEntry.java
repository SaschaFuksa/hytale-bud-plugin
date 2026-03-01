package com.bud.feature.combat;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record OpponentEntry(@Nonnull String entityName, @Nonnull CombatState state, @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    public boolean isAttacked() {
        return state == CombatState.ATTACKED;
    }

    public boolean wasAttacked() {
        return state == CombatState.WAS_ATTACKED;
    }

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
        return entityName;
    }

}
