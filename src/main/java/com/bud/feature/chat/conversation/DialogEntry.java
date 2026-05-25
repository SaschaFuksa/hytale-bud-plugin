package com.bud.feature.chat.conversation;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record DialogEntry(
        @Nonnull String playerName,
        @Nonnull Set<String> participants,
        String previousSpeakerName,
        String previousMessage,
        @Nonnull BudComponent budComponent) implements IQueueEntry, ConversationContext {

    @Override
    public int getPriority() {
        return 3;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return this.budComponent;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return "dialogMode";
    }

    @Nonnull
    @Override
    public String getConversationOwnerKey() {
        return this.playerName;
    }

    @Nonnull
    @Override
    public ConversationMode getConversationMode() {
        return ConversationMode.DIALOG_MODE;
    }

    @Nonnull
    @Override
    public Set<String> getConversationParticipants() {
        return this.participants;
    }

    @Nonnull
    @Override
    public String getConversationInput() {
        if (this.previousSpeakerName == null || this.previousSpeakerName.isBlank()
                || this.previousMessage == null || this.previousMessage.isBlank()) {
            return "Dialog mode has just started. Say one short in-character line to begin a natural conversation with the other Buds while the player listens nearby.";
        }
        return "Previous line from " + this.previousSpeakerName + ": " + this.previousMessage;
    }

    @Nonnull
    public String getDialogInformation() {
        StringBuilder builder = new StringBuilder();
        builder.append("Player nearby: ").append(this.playerName).append(".\n")
                .append("Conversation participants: ").append(String.join(", ", this.participants)).append(".\n")
                .append(this.getConversationInput());
        return builder.toString();
    }
}