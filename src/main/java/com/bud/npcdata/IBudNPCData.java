package com.bud.npcdata;

import com.bud.npcsound.IBudNPCSoundData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.bud.llmmessages.ILLMBudNPCMessage;

public interface IBudNPCData {

    String NPC_TYPE_ID = "";

    ILLMBudNPCMessage getLLMBudNPCMessage();

    IBudNPCSoundData getBudNPCSoundData();

    String getNPCTypeId();

    String getWeaponID();

    String getArmorID();

    void setNPC(NPCEntity npc);

    NPCEntity getNPC();
    
}
