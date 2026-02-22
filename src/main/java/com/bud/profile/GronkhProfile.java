package com.bud.profile;

import javax.annotation.Nonnull;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.profile.sound.GronkhSoundData;
import com.bud.profile.sound.IBudSoundData;
import com.bud.reaction.world.time.DayOfWeek;

public class GronkhProfile implements IBudProfile {

	@Nonnull
	public static final BudType BUD_TYPE_ID = BudType.GRONKH;
	@Nonnull
	public static final String BUD_DISPLAY_NAME = "Gronkh";
	@Nonnull
	private static final IBudSoundData budSoundData = new GronkhSoundData();

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
		return "Weapon_Mace_Stone_Trork";
	}

	@Nonnull
	@Override
	public String getArmorID() {
		return "Armor_Trork_Chest";
	}

	@Nonnull
	@Override
	public BudType getNPCTypeId() {
		return BUD_TYPE_ID;
	}

	@Nonnull
	@Override
	public String getNPCDisplayName() {
		return BUD_DISPLAY_NAME;
	}

	@Nonnull
	@Override
	public DayOfWeek getFavoriteDay() {
		return DayOfWeek.FRIDAY;
	}
}
