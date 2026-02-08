package com.bud.npc.buds;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.buds.sound.IBudSoundData;
import com.bud.npc.buds.sound.KeylethSoundData;

public class KeylethData implements IBudData {

	public static final String NPC_TYPE_ID = "Keyleth_Bud";
	public static final String NPC_DISPLAY_NAME = "Keyleth";
	public static final String NPC_DISPLAY_NAME_LOWER = "keyleth";
	private static final IBudSoundData budSoundData = new KeylethSoundData();

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
		return "Template_Weapon_Shortbow";
	}

	@Override
	public String getArmorID() {
		return "Armor_Kweebec_Chest";
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
