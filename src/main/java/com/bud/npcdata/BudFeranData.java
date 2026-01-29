package com.bud.npcdata;

import com.bud.npcsound.IBudNPCSoundData;
import com.bud.npcsound.BudFeranSoundData;
import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.llmmessages.LLMBudFeranMessage;

public class BudFeranData implements IBudNPCData {

    private final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudFeranMessage();
    private final IBudNPCSoundData budNPCSoundData = new BudFeranSoundData();

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
		return "Feran_Bud";
	}

	@Override
	public String getWeaponID() {
		return "Weapon_Daggers_Bone";
	}

	@Override
	public String getArmorID() {
		return "Armor_Cloth_Cotton_Head";
	}

}
