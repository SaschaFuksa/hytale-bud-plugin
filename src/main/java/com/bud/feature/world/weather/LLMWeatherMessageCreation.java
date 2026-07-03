package com.bud.feature.world.weather;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

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
        if (!(context instanceof WeatherEntry weatherEntry)) {
            throw new IllegalArgumentException("Context must be of type WeatherEntry");
        }
        BudMessage npcMessage = weatherEntry.getBudProfile().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String weatherInfo = weatherEntry.getWeatherInformation();
        String weatherView = npcMessage.getPersonalWeatherView();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("weather")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n").append(weatherView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(weatherInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!weatherEntry.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            weatherEntry.budComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof WeatherEntry weatherEntry)) {
            throw new IllegalArgumentException("Context must be of type WeatherEntry");
        }
        String message = weatherEntry.getBudProfile().getBudMessage()
                .getFallback("weatherView");
        return new Prompt(message, message);
    }
}
