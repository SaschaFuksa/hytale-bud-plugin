package com.bud.core.profiles;

import javax.annotation.Nonnull;

import com.bud.core.sound.GronkhSound;
import com.bud.core.sound.IBudSound;
import com.bud.core.types.BudMessage;
import com.bud.core.types.DayOfWeek;
import com.bud.llm.prompt.LLMPromptManager;

public class GronkhProfile implements IBudProfile {

	@Nonnull
	public static final BudType BUD_TYPE_ID = BudType.GRONKH;
	@Nonnull
	public static final String BUD_DISPLAY_NAME = "Gronkh";
	@Nonnull
	private static final IBudSound budSoundData = new GronkhSound();

	@Nonnull
	@Override
	public BudMessage getBudMessage() {
		return LLMPromptManager.getInstance().getBudMessage(BUD_DISPLAY_NAME);
	}

	@Nonnull
	@Override
	public IBudSound getBudSoundData() {
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
