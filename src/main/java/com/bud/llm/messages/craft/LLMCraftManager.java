package com.bud.llm.messages.craft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.reaction.crafting.CraftEntry;
import com.bud.reaction.crafting.RecentCraftCache;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;

/**
 * LLM manager for crafting reactions.
 * Polls the RecentCraftCache and generates prompts for Buds.
 */
public class LLMCraftManager implements ILLMChatManager {

    private static final LLMCraftManager INSTANCE = new LLMCraftManager();
    public static final String NO_CRAFT_STRING = "No recent crafting events.";

    private final LLMCraftMessageCreation llmCreation;

    private LLMCraftManager() {
        this.llmCreation = new LLMCraftMessageCreation();
    }

    public static LLMCraftManager getInstance() {
        return INSTANCE;
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        CraftEntry latestEntry = (CraftEntry) RecentCraftCache.getInstance()
                .pollHistory(budInstance.getOwner().getUuid());
        if (latestEntry == null) {
            return new DataResult<>(null, NO_CRAFT_STRING);
        }
        LLMCraftContext context = LLMCraftContext.from(latestEntry);
        Prompt prompt = this.llmCreation.createPrompt(context, budInstance);
        return new DataResult<>(prompt, "Craft prompt generation.");
    }

    @Override
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty()) {
            return null;
        }
        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("craftView");
    }
}
