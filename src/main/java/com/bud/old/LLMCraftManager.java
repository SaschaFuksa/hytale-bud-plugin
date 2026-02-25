package com.bud.old;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.feature.crafting.CraftEntry;
import com.bud.feature.crafting.LLMCraftContext;
import com.bud.feature.crafting.RecentCraftCache;
import com.bud.feature.data.npc.BudInstance;
import com.bud.feature.data.npc.BudRegistry;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;

public class LLMCraftManager {

    private static final LLMCraftManager INSTANCE = new LLMCraftManager();
    public static final String NO_CRAFT_STRING = "No recent crafting events.";

    private final LLMCraftMessageCreation llmCreation;

    private LLMCraftManager() {
        this.llmCreation = new LLMCraftMessageCreation();
    }

    public static LLMCraftManager getInstance() {
        return INSTANCE;
    }

    public Prompt generatePrompt(BudInstance budInstance) {
        CraftEntry latestEntry = (CraftEntry) RecentCraftCache.getInstance()
                .pollHistory(budInstance.getOwner().getUuid());
        if (latestEntry == null) {
            return null;
        }
        LLMCraftContext context = LLMCraftContext.from(latestEntry);
        Prompt prompt = this.llmCreation.createPrompt(context, budInstance);
        return prompt;
    }

    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty()) {
            return null;
        }
        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("craftView");
    }
}
