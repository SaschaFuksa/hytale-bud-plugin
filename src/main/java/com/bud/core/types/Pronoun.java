package com.bud.core.types;

import javax.annotation.Nonnull;

public enum Pronoun {

    SHE("she", "her", "her"),
    HE("he", "him", "his"),
    THEY("they", "them", "their");

    @Nonnull
    private final String subject;
    @Nonnull
    private final String object;
    @Nonnull
    private final String possessive;

    Pronoun(@Nonnull String subject, @Nonnull String object, @Nonnull String possessive) {
        this.subject = subject;
        this.object = object;
        this.possessive = possessive;
    }

    @Nonnull
    public String subject() {
        return subject;
    }

    @Nonnull
    public String object() {
        return object;
    }

    @Nonnull
    public String possessive() {
        return possessive;
    }

    @Nonnull
    public String describe(@Nonnull String displayName) {
        return "Refer to " + displayName + " using " + subject + "/" + object + " pronouns.";
    }
}
