package com.bud.profile;

import javax.annotation.Nonnull;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.profile.sound.IBudSoundData;
import com.bud.profile.sound.KeylethSoundData;
import com.bud.reaction.world.time.DayOfWeek;

public class KeylethProfile implements IBudProfile {

	@Nonnull
	private static final BudType BUD_TYPE_ID = BudType.KEYLETH;
	@Nonnull
	private static final String BUD_DISPLAY_NAME = "Keyleth";
	@Nonnull
	private static final IBudSoundData budSoundData = new KeylethSoundData();

	@Nonnull
	@Override
	public BudMessage getBudMessage() {
		return LLMPromptManager.getInstance().getBudMessage(BUD_DISPLAY_NAME);
	}

	@Nonnull
	@Override
	public IBudSoundData getBudSoundData() {
		return budSoundData;
	}

	@Nonnull
	@Override
	public String getWeaponID() {
		return "Template_Weapon_Shortbow";
	}

	@Nonnull
	@Override
	public String getArmorID() {
		return "Armor_Kweebec_Chest";
	}

	@Nonnull
	@Override
	public BudType getNPCTypeId() {
		return BUD_TYPE_ID;
	}

	@Override
	public String getNPCDisplayName() {
		return BUD_DISPLAY_NAME;
	}

	@Override
	public DayOfWeek getFavoriteDay() {
		return DayOfWeek.WEDNESDAY;
	}

}
