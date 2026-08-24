package com.bud.feature.player.state;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record PlayerStateEntry(@Nonnull String effectId, boolean debuff,
        @Nonnull BudComponent budComponent) implements IQueueEntry {

    @Nonnull
    public String getStateInformation() {
        String cleanId = effectId.replace("_", " ");
        return "The player's condition just changed: " + cleanId
                + " (a " + (debuff ? "harmful" : "helpful") + " effect).";
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return "playerState:" + effectId;
    }

}
