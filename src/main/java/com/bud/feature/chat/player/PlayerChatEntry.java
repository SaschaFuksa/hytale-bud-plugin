package com.bud.feature.chat.player;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record PlayerChatEntry(@Nonnull String playerMessage, @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 2;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return "playerChat";
    }

    public String getPlayerChatInformation() {
        return "The player says to you: " + playerMessage;
    }
}
