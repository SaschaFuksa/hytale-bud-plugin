package com.bud.profile;

import javax.annotation.Nonnull;

import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.profile.sound.IBudSoundData;
import com.bud.profile.sound.VeriSoundData;
import com.bud.reaction.world.time.DayOfWeek;

public class VeriProfile implements IBudProfile {

	@Nonnull
	public static final BudType BUD_TYPE_ID = BudType.VERI;
	@Nonnull
	public static final String BUD_DISPLAY_NAME = "Veri";
	@Nonnull
	private static final IBudSoundData budSoundData = new VeriSoundData();

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
