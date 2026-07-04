package com.bud.feature.chat.conversation;

import java.util.Set;

import javax.annotation.Nonnull;

public record ConversationMemoryEntry(
        @Nonnull String summary,
        int importance,
        double effectiveScore,
        @Nonnull String speakerName,
        @Nonnull ConversationMode mode,
        @Nonnull Set<String> participants,
        long createdAt,
        boolean legendary) {

    @Nonnull
    public ConversationMemoryEntry decay(double factor) {
        return new ConversationMemoryEntry(this.summary, this.importance, this.effectiveScore * factor,
                this.speakerName, this.mode, this.participants, this.createdAt, this.legendary);
    }

    @Nonnull
    public String formatForPrompt() {
        return this.speakerName + ": " + this.summary;
    }
}