package com.bud.core.profiles;

import javax.annotation.Nonnull;

import com.bud.core.sound.IBudSound;
import com.bud.core.types.BudMessage;
import com.bud.core.types.DayOfWeek;

public interface IBudProfile {

    @Nonnull
    BudMessage getBudMessage();

    @Nonnull
    IBudSound getBudSoundData();

    @Nonnull
    DayOfWeek getFavoriteDay();

    @Nonnull
    String getWeaponID();

    @Nonnull
    String getArmorID();

    @Nonnull
    BudType getNPCTypeId();

    @Nonnull
    String getNPCDisplayName();

}