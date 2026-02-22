package com.bud.profile;

import javax.annotation.Nonnull;

import com.bud.llm.messages.prompt.BudMessage;
import com.bud.profile.sound.IBudSoundData;
import com.bud.reaction.world.time.DayOfWeek;

public interface IBudProfile {

    BudMessage getBudMessage();

    @Nonnull
    IBudSoundData getBudSoundData();

    DayOfWeek getFavoriteDay();

    String getWeaponID();

    String getArmorID();

    BudType getNPCTypeId();

    String getNPCDisplayName();

}