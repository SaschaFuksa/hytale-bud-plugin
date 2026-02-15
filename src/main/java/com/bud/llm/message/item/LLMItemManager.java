package com.bud.llm.message.item;

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
import com.bud.reaction.item.ItemEntry;
import com.bud.reaction.item.RecentItemCache;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class LLMItemManager implements ILLMChatManager {

    private final LLMItemMessageCreation llmCreation;

    public LLMItemManager() {
        this.llmCreation = new LLMItemMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        PlayerRef player = budInstance.getOwner();
        ItemEntry latestEntry = (ItemEntry) RecentItemCache.getInstance().pollHistory(player.getUuid());

        if (latestEntry == null) {
            return new DataResult<>(null, "No recent item found for player.");
        }

        LLMItemContext contextResult = LLMItemContext.from(latestEntry);
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return new DataResult<>(prompt, "Prompt generation.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        LinkedList<ItemEntry> history = (LinkedList<ItemEntry>) (LinkedList<?>) RecentItemCache.getInstance()
                .getHistory(ownerId);
        if (history == null || history.isEmpty())
            return null;
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));

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
        return manager.getBudMessage(budName.toLowerCase()).getFallback("itemView");
    }
}
