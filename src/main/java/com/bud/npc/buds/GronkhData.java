package com.bud.npc.buds;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.buds.sound.GronkhSoundData;
import com.bud.npc.buds.sound.IBudSoundData;

public class GronkhData implements IBudData {

	public static final String NPC_TYPE_ID = "Gronkh_Bud";
	public static final String NPC_DISPLAY_NAME = "Gronkh";
	private static final IBudSoundData budSoundData = new GronkhSoundData();

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
		return "Weapon_Mace_Stone_Trork";
	}

	@Override
	public String getArmorID() {
		return "Armor_Trork_Chest";
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
