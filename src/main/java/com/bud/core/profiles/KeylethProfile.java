package com.bud.core.profiles;

import javax.annotation.Nonnull;

import com.bud.core.sound.IBudSound;
import com.bud.core.sound.KeylethSound;
import com.bud.core.types.BudMessage;
import com.bud.core.types.DayOfWeek;
import com.bud.llm.prompt.LLMPromptManager;

public class KeylethProfile implements IBudProfile {

	@Nonnull
	private static final BudType BUD_TYPE_ID = BudType.KEYLETH;
	@Nonnull
	private static final String BUD_DISPLAY_NAME = "Keyleth";
	@Nonnull
	private static final IBudSound budSoundData = new KeylethSound();

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

	@Nonnull
	@Override
	public String getNPCDisplayName() {
		return BUD_DISPLAY_NAME;
	}

	@Nonnull
	@Override
	public DayOfWeek getFavoriteDay() {
		return DayOfWeek.WEDNESDAY;
	}

}
