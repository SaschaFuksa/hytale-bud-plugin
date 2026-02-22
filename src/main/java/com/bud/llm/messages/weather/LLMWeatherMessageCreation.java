package com.bud.llm.messages.weather;

import com.bud.llm.AbstractLLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.BudMessage;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.world.time.Mood;

public class LLMWeatherMessageCreation extends AbstractLLMMessageCreation {

    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMWeatherContext weatherContext)) {
            throw new IllegalArgumentException("Context must be of type LLMWeatherContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String weatherInfo = weatherContext.getWeatherInformation();
        String weatherView = npcMessage.getPersonalWeatherView();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("weather")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n").append(weatherView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(weatherInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            budInstance.getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createLLMPrompt(IPromptContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createLLMPrompt'");
    }

    @Override
    protected Prompt createFallbackPrompt(IPromptContext context) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createFallbackPrompt'");
    }
}
