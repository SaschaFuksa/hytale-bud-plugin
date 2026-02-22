package com.bud.llm.messages.state;

import com.bud.llm.AbstractLLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.BudMessage;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.reaction.world.time.Mood;

public class LLMStateMessageCreation extends AbstractLLMMessageCreation {

    private static final LLMStateMessageCreation INSTANCE = new LLMStateMessageCreation();

    private LLMStateMessageCreation() {
    }

    public static LLMStateMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(IPromptContext context) {
        if (!(context instanceof LLMStateContext stateContext)) {
            throw new IllegalArgumentException("Context must be of type LLMStateContext");
        }
        BudMessage npcMessage = stateContext.budProfile().getBudMessage();

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
                .append(stateInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!context.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            context.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(IPromptContext context) {
        if (!(context instanceof LLMStateContext stateContext)) {
            throw new IllegalArgumentException("Context must be of type LLMStateContext");
        }
        String message = stateContext.budProfile().getBudMessage()
                .getFallback(stateContext.getBudComponent().getCurrentState().getStateName());
        return new Prompt(message, message);
    }

}
