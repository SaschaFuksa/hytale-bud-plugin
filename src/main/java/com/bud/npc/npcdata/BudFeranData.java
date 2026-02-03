package com.bud.npc.npcdata;

import com.bud.npc.npcsound.IBudNPCSoundData;
import com.bud.llm.llmbudmessage.ILLMBudNPCMessage;
import com.bud.llm.llmbudmessage.LLMBudFeranMessage;
import com.bud.npc.npcsound.BudFeranSoundData;

public class BudFeranData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Veri_Bud";
	public static final String NPC_DISPLAY_NAME = "Veri";
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
	public String getWeaponID() {
		return "Weapon_Daggers_Bone";
	}

	@Override
	public String getArmorID() {
		return "Debug_Armor_D_Head_Of_Dash_Reduction";
	}

	@Override
	public String getNPCTypeId() {
		return NPC_TYPE_ID;
	}

	@Override
	public String getNPCDisplayName() {
		return NPC_DISPLAY_NAME;
	}

}
