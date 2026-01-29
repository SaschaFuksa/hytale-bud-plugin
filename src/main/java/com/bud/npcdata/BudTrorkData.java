package com.bud.npcdata;

import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.llmmessages.LLMBudTrorkMessage;
import com.bud.npcsound.BudTrorkSoundData;
import com.bud.npcsound.IBudNPCSoundData;

public class BudTrorkData implements IBudNPCData {

    private final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudTrorkMessage();
    private final IBudNPCSoundData budNPCSoundData = new BudTrorkSoundData();

    @Override
    public ILLMBudNPCMessage getLLMBudNPCMessage() {
        return llmBudNPCMessage;
    }

    @Override
    public IBudNPCSoundData getBudNPCSoundData() {
        return budNPCSoundData;
    }

	@Override
	public String getNPCTypeId() {
		return "Trork_Bud";
	}

	@Override
	public String getWeaponID() {
		return "Weapon_Mace_Stone_Trork";
	}

	@Override
	public String getArmorID() {
		return "Armor_Trork_Chest";
	}
    
}
