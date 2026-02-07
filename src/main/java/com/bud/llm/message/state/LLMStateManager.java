package com.bud.llm.message.state;

import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.world.LLMWorldInfoMessageCreation;
import com.bud.npc.BudInstance;
import com.bud.result.IDataResult;

public class LLMStateManager implements ILLMChatManager {

    private final LLMWorldInfoMessageCreation llmCreation;

    public LLMStateManager() {
        this.llmCreation = new LLMWorldInfoMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generatePrompt'");
    }

    @Override
    public BudInstance getBudInstance(UUID ownerId) {
        throw new UnsupportedOperationException("Unimplemented method 'getRandomInstanceForOwner'");
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFallbackMessage'");
    }

}
