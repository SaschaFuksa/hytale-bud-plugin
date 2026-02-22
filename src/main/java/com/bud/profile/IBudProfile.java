package com.bud.profile;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.profile.sound.IBudSoundData;
import com.bud.reaction.world.time.DayOfWeek;

public interface IBudProfile {

    BudMessage getBudMessage();

    IBudSoundData getBudSoundData();

    DayOfWeek getFavoriteDay();

    String getWeaponID();

    String getArmorID();

    BudType getNPCTypeId();

    String getNPCDisplayName();

}