package com.bud.core.sound;

import javax.annotation.Nonnull;

import com.bud.core.types.BudState;

public interface IBudSound {

    @Nonnull
    String getDefensiveSound();

    @Nonnull
    String getPassiveSound();

    @Nonnull
    String getSittingSound();

    @Nonnull
    String getSoundForState(BudState state);

}
