package com.bud.npcdata;

import com.bud.llmmessage.ILLMBudNPCMessage;
import com.bud.llmmessage.LLMBudTrorkMessage;
import com.bud.npcsound.BudTrorkSoundData;
import com.bud.npcsound.IBudNPCSoundData;

public class BudTrorkData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Gronkh_Bud";
    private static final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudTrorkMessage();
    private static final IBudNPCSoundData budNPCSoundData = new BudTrorkSoundData();

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
		return NPC_TYPE_ID;
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
