package com.bud.feature.chat.player;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.chat.conversation.ConversationContext;
import com.bud.feature.chat.conversation.ConversationMode;
import com.bud.feature.queue.IQueueEntry;

public record PlayerChatEntry(@Nonnull String playerMessage, @Nonnull BudComponent budComponent)
        implements IQueueEntry, ConversationContext {

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

    @Nonnull
    @Override
    public String getConversationOwnerKey() {
        return this.budComponent.getPlayerRef().getUsername();
    }

    @Nonnull
    @Override
    public ConversationMode getConversationMode() {
        return ConversationMode.DIRECT_CHAT;
    }

    @Nonnull
    @Override
    public Set<String> getConversationParticipants() {
        return Objects.requireNonNull(
                Set.of(this.budComponent.getPlayerRef().getUsername(), this.getBudProfile().getNPCDisplayName()));
    }

    @Nonnull
    @Override
    public String getConversationInput() {
        return this.playerMessage;
    }
}
