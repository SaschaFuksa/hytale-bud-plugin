package com.bud.npc.buds;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.npc.buds.sound.IBudSoundData;

public interface IBudData {

    BudMessage getBudMessage();

    IBudSoundData getBudSoundData();

    String getWeaponID();

    String getArmorID();

    String getNPCTypeId();

    String getNPCDisplayName();

}