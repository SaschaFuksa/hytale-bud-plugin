package com.bud.llm.message.mood;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;

public class LLMMoodManager implements ILLMChatManager {

    private final LLMMoodMessageCreation llmCreation;

    public LLMMoodManager() {
        this.llmCreation = new LLMMoodMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        LLMMoodContext contextResult = LLMMoodContext.from();
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
