package com.bud.feature.bud.reaction;

import javax.annotation.Nonnull;

public enum BudReactionKind {

    MOOD_CHANGE("budMoodReaction"),
    GREETING("budGreeting"),
    LEGENDARY_MEMORY("budLegendaryReaction"),
    STATE_CHANGE("budStateReaction"),
    NAMED_REPLY("budNamedReply");

    @Nonnull
    private final String systemPromptKey;

    BudReactionKind(@Nonnull String systemPromptKey) {
        this.systemPromptKey = systemPromptKey;
    }

    @Nonnull
    public String getSystemPromptKey() {
        return systemPromptKey;
    }
}
