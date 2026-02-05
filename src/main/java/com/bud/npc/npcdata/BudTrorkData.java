package com.bud.npc.npcdata;

import com.bud.llm.llmmessage.BudLLMMessage;
import com.bud.llm.llmmessage.BudLLMPromptManager;
import com.bud.npc.npcsound.BudTrorkSoundData;
import com.bud.npc.npcsound.IBudNPCSoundData;

public class BudTrorkData implements IBudNPCData {

    public static final String NPC_TYPE_ID = "Gronkh_Bud";
    public static final String NPC_DISPLAY_NAME = "Gronkh";
    private static final IBudNPCSoundData budNPCSoundData = new BudTrorkSoundData();

    @Override
    public BudLLMMessage getLLMBudNPCMessage() {
        return BudLLMPromptManager.getInstance().getBudMessage(NPC_DISPLAY_NAME);
    }

    @Override
    public IBudNPCSoundData getBudNPCSoundData() {
		return budNPCSoundData;
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
