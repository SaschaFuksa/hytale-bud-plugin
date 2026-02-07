package com.bud.npc.buds;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.buds.sound.VeriSoundData;
import com.bud.npc.buds.sound.IBudSoundData;

public class VeriData implements IBudData {

	public static final String NPC_TYPE_ID = "Veri_Bud";
	public static final String NPC_DISPLAY_NAME = "Veri";
	private static final IBudSoundData budSoundData = new VeriSoundData();

	@Override
	public BudMessage getBudMessage() {
		return LLMPromptManager.getInstance().getBudMessage(NPC_DISPLAY_NAME);
	}

	@Override
	public IBudSoundData getBudSoundData() {
		return budSoundData;
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
