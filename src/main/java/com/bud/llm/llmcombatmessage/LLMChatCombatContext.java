package com.bud.llm.llmcombatmessage;

import java.util.LinkedList;

import com.bud.llm.BudLLMRandomChat;
import com.bud.llm.ILLMChatContext;
import com.bud.llm.llmmessage.ILLMBudNPCMessage;
import com.bud.npc.BudInstance;
import com.bud.npc.npcdata.IBudNPCData;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.bud.system.RecentOpponentCache;
import com.bud.system.RecentOpponentCache.OpponentEntry;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class LLMChatCombatContext implements ILLMChatContext {

    @Override
    public IDataResult<String> generatePrompt(BudInstance budInstance) {
        PlayerRef player = budInstance.getOwner();
        LinkedList<OpponentEntry> history = RecentOpponentCache.getHistory(player.getUuid());
        if (history.isEmpty()) {
            return new DataResult<>(null, BudLLMRandomChat.NO_COMBAT_STRING);
        }
        System.out.println("[BUD] Generating combat prompt for " + budInstance.getEntity().getNPCTypeId() + ".");
        System.out.println("[BUD] Generating combat prompt for " + history.size() + " entries.");

        IBudNPCData budNPCData = budInstance.getData();

        if (budNPCData == null)
            return new DataResult<>(null, "No NPC data available.");

        ILLMBudNPCMessage npcMessage = budNPCData.getLLMBudNPCMessage();

        OpponentEntry latestEntry = history.getFirst();
        System.out.println("[BUD] Latest combat entry: " + latestEntry.roleName() + ", state: " + latestEntry.state());
        String combatHistory = buildCombatPrompt(latestEntry);
        String prompt = LLMCombatMessageManager.createPrompt(combatHistory, npcMessage, latestEntry.roleName());
        history.removeFirst();
        return new DataResult<>(prompt, "Prompt generated successfully.");
    }

    private String buildCombatPrompt(OpponentEntry latestEntry) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Recent combat interactions:\n");
        String action = switch (latestEntry.state()) {
            case ATTACKED -> "attacked";
            case WAS_ATTACKED -> "run away from";
        };
        String roleName = latestEntry.roleName().replace("Bud_", "");
        roleName = roleName.replace("_", " ");
        promptBuilder.append("Your Buddy ").append(action).append(" ").append(roleName).append(".\n");
        return promptBuilder.toString();
    }

}
