package com.bud.npc.npcdata;

import com.bud.llm.llmbudmessage.ILLMBudNPCMessage;
import com.bud.npc.npcsound.IBudNPCSoundData;

public interface IBudNPCData {

    ILLMBudNPCMessage getLLMBudNPCMessage();

    IBudNPCSoundData getBudNPCSoundData();

    String getWeaponID();

    String getArmorID();

    String getNPCTypeId();

    String getNPCDisplayName();

}