package com.bud.old;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.feature.data.npc.BudInstance;
import com.bud.feature.data.npc.BudRegistry;
import com.bud.feature.item.ItemEntry;
import com.bud.feature.item.RecentItemCache;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class LLMItemManager {

    private final LLMItemMessageCreation llmCreation;

    public LLMItemManager() {
        this.llmCreation = new LLMItemMessageCreation();
    }

    public Prompt generatePrompt(BudInstance budInstance) {
        PlayerRef player = budInstance.getOwner();
        ItemEntry latestEntry = (ItemEntry) RecentItemCache.getInstance().pollHistory(player.getUuid());

        if (latestEntry == null) {
            return null;

        }

        LLMItemContext contextResult = LLMItemContext.from(latestEntry);
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return prompt;
    }

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

    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("itemView");
    }
}
