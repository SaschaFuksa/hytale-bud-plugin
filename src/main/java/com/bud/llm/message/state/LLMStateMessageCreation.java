package com.bud.llm.message.state;

import com.bud.llm.message.creation.ILLMMessageCreation;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;

public class LLMStateMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudMessage npcMessage) {
        if (!(context instanceof LLMStateContext stateContext)) {
            throw new IllegalArgumentException("Context must be of type LLMStateContext");
        }

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getSystemPrompt();
        String stateInfo = stateContext.getStateInformation();
        String stateView = npcMessage.getState(stateContext.state());

        String systemPrompt = manager.getSystemPrompt("state") + "\n" + manager.getSystemPrompt("default") + "\n"
                + budInfo;
        String message = stateView + "\n" + stateInfo;

        return new Prompt(systemPrompt, message);
    }

}
