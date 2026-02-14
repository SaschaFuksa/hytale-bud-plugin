package com.bud.npc.buds;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.npc.buds.sound.IBudSoundData;
import com.bud.reaction.world.time.DayOfWeek;

public interface IBudData {

    BudMessage getBudMessage();

    IBudSoundData getBudSoundData();

    DayOfWeek getFavoriteDay();

    String getWeaponID();

    String getArmorID();

    String getNPCTypeId();

    String getNPCDisplayName();

}