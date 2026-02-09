package com.bud.llm.message.state;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;

public class LLMStateMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMStateContext stateContext)) {
            throw new IllegalArgumentException("Context must be of type LLMStateContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String stateInfo = stateContext.getStateInformation();
        String stateView = npcMessage.getState(stateContext.state());

        String systemPrompt = manager.getSystemPrompt("state") + "\n" + manager.getSystemPrompt("default") + "\n"
                + budInfo;
        String message = stateView + "\n" + stateInfo + "\n" + manager.getSystemPrompt("final");

        return new Prompt(systemPrompt, message);
    }

}
