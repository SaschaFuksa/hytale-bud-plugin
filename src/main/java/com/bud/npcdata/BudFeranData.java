package com.bud.npcdata;

import com.bud.npcsound.IBudNPCSoundData;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.bud.npcsound.BudFeranSoundData;
import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.llmmessages.LLMBudFeranMessage;

public class BudFeranData implements IBudNPCData {

	public static final String NPC_TYPE_ID = "Feran_Bud";
    private final ILLMBudNPCMessage llmBudNPCMessage = new LLMBudFeranMessage();
    private final IBudNPCSoundData budNPCSoundData = new BudFeranSoundData();
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
		return "Weapon_Daggers_Bone";
	}

	@Override
	public String getArmorID() {
		return "Armor_Cloth_Cotton_Head";
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
