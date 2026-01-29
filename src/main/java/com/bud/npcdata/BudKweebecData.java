package com.bud.npcdata;

import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.llmmessages.LLMBudKweebecMessage;
import com.bud.npcsound.BudKweebecSoundData;
import com.bud.npcsound.IBudNPCSoundData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudKweebecData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Kacche_Bud";
    private final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudKweebecMessage();
    private final IBudNPCSoundData budNPCSoundData = new BudKweebecSoundData();
	private NPCEntity npc;

    @Override
    public ILLMBudNPCMessage getLLMBudNPCMessage() {
        return llmBudNPCMessage;
    }

    @Override
    public IBudNPCSoundData getBudNPCSoundData() {
        return budNPCSoundData;
    }

	@Override
	public String getNPCTypeId() {
		return NPC_TYPE_ID;
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
	public void setNPC(NPCEntity npc) {
		this.npc = npc;
	}

	@Override
	public NPCEntity getNPC() {
		return this.npc;
	}
}
