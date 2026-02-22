package com.bud.llm.messages.discover;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.reaction.discover.DiscoverEntry;
import com.bud.reaction.discover.RecentDiscoverCache;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;

/**
 * LLM manager for zone discovery reactions.
 * Polls the RecentDiscoverCache and generates prompts for Buds.
 */
public class LLMDiscoverManager implements ILLMChatManager {

    private static final LLMDiscoverManager INSTANCE = new LLMDiscoverManager();
    public static final String NO_DISCOVER_STRING = "No recent zone discoveries.";

    private final LLMDiscoverMessageCreation llmCreation;

    private LLMDiscoverManager() {
        this.llmCreation = new LLMDiscoverMessageCreation();
    }

    public static LLMDiscoverManager getInstance() {
        return INSTANCE;
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        DiscoverEntry latestEntry = (DiscoverEntry) RecentDiscoverCache.getInstance()
                .pollHistory(budInstance.getOwner().getUuid());
        if (latestEntry == null) {
            return new DataResult<>(null, NO_DISCOVER_STRING);
        }
        LLMDiscoverContext context = LLMDiscoverContext.from(latestEntry);
        Prompt prompt = this.llmCreation.createPrompt(context, budInstance);
        return new DataResult<>(prompt, "Discover prompt generation.");
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
        return manager.getBudMessage(budName.toLowerCase()).getFallback("discoverView");
    }
}
