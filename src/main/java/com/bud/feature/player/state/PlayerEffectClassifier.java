package com.bud.feature.player.state;

import javax.annotation.Nonnull;

public final class PlayerEffectClassifier {

    public enum Category {
        POSITIVE,
        NEGATIVE,
        NEUTRAL
    }

    private static final String[] NEGATIVE_MARKERS = {
            "poison", "burn", "freeze", "slow", "root", "stun", "broken", "drain", "error"
    };

    private static final String[] POSITIVE_MARKERS = {
            "potion", "food", "regen", "boost", "restore", "heal", "buff", "antidote", "immun"
    };

    private PlayerEffectClassifier() {
    }

    @Nonnull
    public static Category classify(@Nonnull String effectId) {
        String lower = effectId.toLowerCase();
        for (String marker : NEGATIVE_MARKERS) {
            if (lower.contains(marker)) {
                return Category.NEGATIVE;
            }
        }
        for (String marker : POSITIVE_MARKERS) {
            if (lower.contains(marker)) {
                return Category.POSITIVE;
            }
        }
        return Category.NEUTRAL;
    }
}
