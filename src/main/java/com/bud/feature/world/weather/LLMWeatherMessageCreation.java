package com.bud.feature.world.weather;

import javax.annotation.Nonnull;

import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;
import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;

public class LLMWeatherMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMWeatherMessageCreation INSTANCE = new LLMWeatherMessageCreation();

    private LLMWeatherMessageCreation() {
    }

    @Nonnull
    public static LLMWeatherMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMWeatherContext weatherContext)) {
            throw new IllegalArgumentException("Context must be of type LLMWeatherContext");
        }
        BudMessage npcMessage = weatherContext.getBudProfile().getBudMessage();

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

        if (!weatherContext.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            weatherContext.budComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMWeatherContext weatherContext)) {
            throw new IllegalArgumentException("Context must be of type LLMWeatherContext");
        }
        String message = weatherContext.getBudProfile().getBudMessage()
                .getFallback("weatherView");
        return new Prompt(message, message);
    }
}
