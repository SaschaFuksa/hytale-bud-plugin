package com.bud.core.types;

import java.util.Random;

public enum Mood {
    DEFAULT("Neutral"),
    SAD("Sad"),
    INSANE("Insane"),
    GRUMPY("Grumpy"),
    DAZED("Dazed"),
    OVERMOTIVATED("Overmotivated");

    private final String displayName;

    private static final Random RANDOM = new Random();

    Mood(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Mood getRandomMood() {
        Mood[] moods = { SAD, INSANE, GRUMPY, DAZED };
        return moods[RANDOM.nextInt(moods.length)];
    }
}
