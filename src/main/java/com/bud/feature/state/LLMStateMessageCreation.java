package com.bud.feature.state;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMStateMessageCreation extends AbstractLLMMessageCreation {

    private static final LLMStateMessageCreation INSTANCE = new LLMStateMessageCreation();

    private LLMStateMessageCreation() {
    }

    @Nonnull
    public static LLMStateMessageCreation getInstance() {
        if (INSTANCE == null) {
            return new LLMStateMessageCreation();
        }
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMStateContext stateContext)) {
            throw new IllegalArgumentException("Context must be of type LLMStateContext");
        }
        BudMessage npcMessage = stateContext.getBudProfile().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String stateInfo = stateContext.getStateInformation();
        String stateView = npcMessage.getState(stateContext.getBudComponent().getCurrentState().getStateName());

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("state")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(stateView).append("\n")
                .append(stateInfo).append("\n");

        if (!stateContext.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            stateContext.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }
        messageBuilder.append("\n").append(manager.getSystemPrompt("final"));

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMStateContext stateContext)) {
            throw new IllegalArgumentException("Context must be of type LLMStateContext");
        }
        String message = stateContext.getBudProfile().getBudMessage()
                .getFallback(stateContext.getBudComponent().getCurrentState().getStateName());
        return new Prompt(message, message);
    }

}
