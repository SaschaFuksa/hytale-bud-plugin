package com.bud.llm.message.block;

import com.bud.llm.message.creation.ILLMMessageCreation;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;

public class LLMBlockMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudMessage npcMessage) {
        if (!(context instanceof LLMBlockContext blockContext)) {
            throw new IllegalArgumentException("Context must be of type LLMBlockContext");
        }

        LLMPromptManager manager = LLMPromptManager.getInstance();

        String playerName = blockContext.player().getUsername();
        String blockName = blockContext.blockName();

        // Simple context message for the LLM
        String interactionInfo = String.format("The player %s just broke a block: %s.", playerName, blockName);

        String budInfo = npcMessage.getCharacteristics();
        String personalView = npcMessage.getPersonalBlockView();
        if (personalView == null)
            personalView = npcMessage.getPersonalWorldView(); // Fallback

        String systemPrompt = manager.getSystemPrompt("block") + "\n"
                + manager.getSystemPrompt("default") + "\n" + budInfo + "\n"
                + personalView;

        String message = interactionInfo + "\n" + manager.getSystemPrompt("final");
        return new Prompt(systemPrompt, message);
    }
}
