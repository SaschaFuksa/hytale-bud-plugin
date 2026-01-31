package com.bud.npcdata;

import com.bud.llmmessage.ILLMBudNPCMessage;
import com.bud.llmmessage.LLMBudKweebecMessage;
import com.bud.npcsound.BudKweebecSoundData;
import com.bud.npcsound.IBudNPCSoundData;

public class BudKweebecData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Kacche_Bud";
	public static final String NPC_DISPLAY_NAME = "Keyleth";
    private static final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudKweebecMessage();
    private static final IBudNPCSoundData budNPCSoundData = new BudKweebecSoundData();

    @Override
    public ILLMBudNPCMessage getLLMBudNPCMessage() {
        return llmBudNPCMessage;
    }

    @Override
    public IBudNPCSoundData getBudNPCSoundData() {
        return budNPCSoundData;
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
