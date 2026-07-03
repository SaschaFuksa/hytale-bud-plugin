package com.bud.feature.world.env;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMWorldMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMWorldMessageCreation INSTANCE = new LLMWorldMessageCreation();

    private LLMWorldMessageCreation() {
    }

    @Nonnull
    public static LLMWorldMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof WorldEntry worldEntry)) {
            throw new IllegalArgumentException("Context must be of type WorldEntry");
        }
        BudMessage npcMessage = worldEntry.getBudProfile().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        WorldMessage template = manager.getWorldInfoTemplate();

        ZoneMessage zoneMessage = worldEntry.getZoneInfo(manager);
        String zoneInfo = zoneMessage != null ? zoneMessage.getZone() : "Unknown Zone";
        String biomeInfo = zoneMessage != null ? worldEntry.getBiomeInfo(zoneMessage) : "Unknown Biome";
        String timeInfo = worldEntry.getTimeInfo(manager.getTimeMessage());
        String budInfo = npcMessage.getCharacteristics();
        String environmentInfo = template.getEnvironmentInfo().formatted(zoneInfo, biomeInfo, timeInfo);
        String weatherInfo = worldEntry.getWeatherInfo();
        String personalView = npcMessage.getPersonalWorldView();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("world")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n")
                .append(personalView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(environmentInfo).append("\n")
                .append(weatherInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!worldEntry.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            worldEntry.budComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof WorldEntry worldEntry)) {
            throw new IllegalArgumentException("Context must be of type WorldEntry");
        }
        String message = worldEntry.getBudProfile().getBudMessage()
                .getFallback("worldView");
        return new Prompt(message, message);
    }
}
