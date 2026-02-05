package com.bud.llm.llmmessage.llmcombatmessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.bud.llm.BudLLMRandomChat;
import com.bud.llm.ILLMChatContext;
import com.bud.llm.llmmessage.BudLLMMessage;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.npc.npcdata.IBudNPCData;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.bud.system.RecentOpponentCache;
import com.bud.system.RecentOpponentCache.OpponentEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class LLMChatCombatContext implements ILLMChatContext {

    @Override
    public IDataResult<String> generatePrompt(BudInstance budInstance) {
        PlayerRef player = budInstance.getOwner();

        // Use pollHistory to get and remove the entry atomically
        // This ensures that only one Bud processes this specific combat event
        OpponentEntry latestEntry = RecentOpponentCache.pollHistory(player.getUuid());

        if (latestEntry == null) {
            return new DataResult<>(null, BudLLMRandomChat.NO_COMBAT_STRING);
        }

        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Generating combat prompt for " + budInstance.getEntity().getNPCTypeId() + ".");
        LoggerUtil.getLogger().fine(
                () -> "[BUD] Processing combat entry: " + latestEntry.roleName() + ", state: " + latestEntry.state());

        IBudNPCData budNPCData = budInstance.getData();

        if (budNPCData == null)
            return new DataResult<>(null, "No NPC data available.");

        BudLLMMessage npcMessage = budNPCData.getLLMBudNPCMessage();

        String combatHistory = buildCombatPrompt(latestEntry);
        String prompt = LLMCombatMessageManager.createPrompt(combatHistory, npcMessage, latestEntry.roleName());

        return new DataResult<>(prompt, "Prompt generated successfully.");
    }

    @Override
    public BudInstance getRandomInstanceForOwner(UUID ownerId) {
        // Peek at history without removing - generatePrompt will do the atomic poll
        LinkedList<OpponentEntry> history = RecentOpponentCache.getHistory(ownerId);
        if (history == null || history.isEmpty())
            return null;

        OpponentEntry latestEntry = history.getFirst();
        String roleName = latestEntry.roleName();

        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));

        // Filter out the bud that matches the roleName of the opponent (avoid talking
        // about itself as an opponent)
        ownerBuds.removeIf(bud -> {
            String npcTypeId = bud.getEntity().getNPCTypeId();
            return npcTypeId != null && npcTypeId.equals(roleName);
        });

        if (ownerBuds.isEmpty()) {
            // No suitable Buds available - let generatePrompt handle cleanup
            // Don't poll here to avoid race condition with generatePrompt
            return null;
        }

        return ownerBuds.get((int) (Math.random() * ownerBuds.size()));
    }

    private String buildCombatPrompt(OpponentEntry latestEntry) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Recent combat interactions:\n");
        String action = switch (latestEntry.state()) {
            case ATTACKED -> "attacked";
            case WAS_ATTACKED -> "run away from";
        };
        String roleName = latestEntry.roleName().replace("_Bud", "");
        roleName = roleName.replace("_", " ");
        promptBuilder.append("Your Buddy ").append(action).append(" ").append(roleName).append(".\n");
        return promptBuilder.toString();
    }

}
