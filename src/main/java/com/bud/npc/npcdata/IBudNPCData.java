package com.bud.npc.npcdata;

import com.bud.npc.npcsound.IBudNPCSoundData;
import com.bud.llm.llmmessage.ILLMBudNPCMessage;

public interface IBudNPCData {

    ILLMBudNPCMessage getLLMBudNPCMessage();

    IBudNPCSoundData getBudNPCSoundData();

    String getWeaponID();

    String getArmorID();

    String getNPCTypeId();

    String getNPCDisplayName();

}