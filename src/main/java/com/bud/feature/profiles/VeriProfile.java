package com.bud.feature.profiles;

import javax.annotation.Nonnull;

import com.bud.core.sound.IBudSound;
import com.bud.core.sound.VeriSound;
import com.bud.core.types.BudType;
import com.bud.core.types.DayOfWeek;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.profiles.IBudProfile;

public class VeriProfile implements IBudProfile {

	@Nonnull
	public static final BudType BUD_TYPE_ID = BudType.VERI;
	@Nonnull
	public static final String BUD_DISPLAY_NAME = "Veri";
	@Nonnull
	private static final IBudSound budSoundData = new VeriSound();

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
		return "Weapon_Daggers_Bone";
	}

	@Nonnull
	@Override
	public String getArmorID() {
		return "Debug_Armor_D_Head_Of_Dash_Reduction";
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
		return DayOfWeek.MONDAY;
	}

}
