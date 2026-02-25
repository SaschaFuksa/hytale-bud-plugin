package com.bud.llm.profiles;

import javax.annotation.Nonnull;

import com.bud.core.sound.IBudSound;
import com.bud.core.types.BudType;
import com.bud.core.types.DayOfWeek;
import com.bud.llm.messages.BudMessage;

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