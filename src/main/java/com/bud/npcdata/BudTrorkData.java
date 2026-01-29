package com.bud.npcdata;

import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.llmmessages.LLMBudTrorkMessage;
import com.bud.npcsound.BudTrorkSoundData;
import com.bud.npcsound.IBudNPCSoundData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudTrorkData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Trork_Bud";
    private final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudTrorkMessage();
    private final IBudNPCSoundData budNPCSoundData = new BudTrorkSoundData();
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
		return "Weapon_Mace_Stone_Trork";
	}

	@Override
	public String getArmorID() {
		return "Armor_Trork_Chest";
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
