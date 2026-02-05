package com.bud.npc.npcdata;

import com.bud.llm.llmmessage.BudLLMMessage;
import com.bud.npc.npcsound.IBudNPCSoundData;

public interface IBudNPCData {

    BudLLMMessage getLLMBudNPCMessage();

    IBudNPCSoundData getBudNPCSoundData();

    String getWeaponID();

    String getArmorID();

    String getNPCTypeId();

    String getNPCDisplayName();

}