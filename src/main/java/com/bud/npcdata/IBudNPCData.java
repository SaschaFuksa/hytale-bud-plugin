package com.bud.npcdata;

import com.bud.npcsound.IBudNPCSoundData;
import com.bud.llmmessage.ILLMBudNPCMessage;

public interface IBudNPCData {

    ILLMBudNPCMessage getLLMBudNPCMessage();

    IBudNPCSoundData getBudNPCSoundData();

    String getWeaponID();
    
    String getArmorID();
    
    String getNPCTypeId();
    
    String getNPCDisplayName();

}