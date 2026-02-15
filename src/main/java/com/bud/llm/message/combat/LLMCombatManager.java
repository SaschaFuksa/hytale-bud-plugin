package com.bud.llm.message.combat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.reaction.combat.OpponentEntry;
import com.bud.reaction.combat.RecentOpponentCache;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class LLMCombatManager implements ILLMChatManager {

    private final LLMCombatMessageCreation llmCreation;

    public LLMCombatManager() {
        this.llmCreation = new LLMCombatMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        PlayerRef player = budInstance.getOwner();
        OpponentEntry latestEntry = (OpponentEntry) RecentOpponentCache.getInstance().pollHistory(player.getUuid());
        if (latestEntry == null) {
            return new DataResult<>(null, "No recent combat data for player.");
        }
        LLMCombatContext contextResult = LLMCombatContext.from(latestEntry, player);
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return new DataResult<>(prompt, "Prompt generation.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        // Peek at history without removing - generatePrompt will do the atomic poll
        LinkedList<OpponentEntry> history = (LinkedList<OpponentEntry>) (LinkedList<?>) RecentOpponentCache
                .getInstance()
                .getHistory(ownerId);
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

        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("combatView");
    }

}
