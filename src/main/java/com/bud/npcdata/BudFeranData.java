package com.bud.npcdata;

import com.bud.npcsound.IBudNPCSoundData;
import com.bud.npcsound.BudFeranSoundData;
import com.bud.llmmessage.ILLMBudNPCMessage;
import com.bud.llmmessage.LLMBudFeranMessage;

public class BudFeranData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Veri_Bud";
    private static final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudFeranMessage();
    private static final IBudNPCSoundData budNPCSoundData = new BudFeranSoundData();

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
		return "Weapon_Daggers_Bone";
	}

	@Override
	public String getArmorID() {
		return "Debug_Armor_D_Head_Of_Dash_Reduction";
	}

}
