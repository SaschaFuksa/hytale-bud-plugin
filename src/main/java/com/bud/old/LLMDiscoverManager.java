package com.bud.old;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.feature.data.npc.BudInstance;
import com.bud.feature.data.npc.BudRegistry;
import com.bud.feature.discover.DiscoverEntry;
import com.bud.feature.discover.RecentDiscoverCache;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;

/**
 * LLM manager for zone discovery reactions.
 * Polls the RecentDiscoverCache and generates prompts for Buds.
 */
public class LLMDiscoverManager {

    private static final LLMDiscoverManager INSTANCE = new LLMDiscoverManager();
    public static final String NO_DISCOVER_STRING = "No recent zone discoveries.";

    private final LLMDiscoverMessageCreation llmCreation;

    private LLMDiscoverManager() {
        this.llmCreation = new LLMDiscoverMessageCreation();
    }

    public static LLMDiscoverManager getInstance() {
        return INSTANCE;
    }

    public Prompt generatePrompt(BudInstance budInstance) {
        DiscoverEntry latestEntry = (DiscoverEntry) RecentDiscoverCache.getInstance()
                .pollHistory(budInstance.getOwner().getUuid());
        if (latestEntry == null) {
            return null;
        }
        LLMDiscoverContext context = LLMDiscoverContext.from(latestEntry);
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
        return manager.getBudMessage(budName.toLowerCase()).getFallback("discoverView");
    }
}
