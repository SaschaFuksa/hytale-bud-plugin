package com.bud.llm.messages.favoriteday;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;

public class LLMFavoriteDayManager implements ILLMChatManager {

    private final LLMFavoriteDayMessageCreation llmCreation;

    public LLMFavoriteDayManager() {
        this.llmCreation = new LLMFavoriteDayMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        LLMFavoriteDayContext contextResult = LLMFavoriteDayContext.from();
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance);
        return new DataResult<>(prompt, "Prompt generation.");
    }

    @Override
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        return Collections.emptySet();
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("favoriteDayView");
    }

}
