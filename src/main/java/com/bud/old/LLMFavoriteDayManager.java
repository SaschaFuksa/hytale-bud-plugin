package com.bud.old;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.bud.feature.bud.LLMFavoriteDayContext;
import com.bud.feature.data.npc.BudInstance;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;

public class LLMFavoriteDayManager {

    private final LLMFavoriteDayMessageCreation llmCreation;

    public LLMFavoriteDayManager() {
        this.llmCreation = new LLMFavoriteDayMessageCreation();
    }

    public Prompt generatePrompt(BudInstance budInstance) {
        LLMFavoriteDayContext contextResult = LLMFavoriteDayContext.from();
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return prompt;
    }

    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        return Collections.emptySet();
    }

    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("favoriteDayView");
    }

}
