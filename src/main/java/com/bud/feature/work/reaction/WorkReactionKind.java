package com.bud.feature.work.reaction;

import javax.annotation.Nonnull;

public enum WorkReactionKind {

    START("workStart", "workViewStart", 3),
    END("workEnd", "workViewEnd", 3),
    INTERACT("workInteract", "workViewInteract", 2),
    OUTPUT_FULL("workOutputFull", "workViewOutputFull", 3),
    OUT_OF_FUEL("workOutOfFuel", "workViewOutOfFuel", 3);

    @Nonnull
    private final String systemPromptKey;

    @Nonnull
    private final String fallbackKey;

    private final int priority;

    WorkReactionKind(@Nonnull String systemPromptKey, @Nonnull String fallbackKey, int priority) {
        this.systemPromptKey = systemPromptKey;
        this.fallbackKey = fallbackKey;
        this.priority = priority;
    }

    @Nonnull
    public String getSystemPromptKey() {
        return systemPromptKey;
    }

    @Nonnull
    public String getFallbackKey() {
        return fallbackKey;
    }

    public int getPriority() {
        return priority;
    }
}
