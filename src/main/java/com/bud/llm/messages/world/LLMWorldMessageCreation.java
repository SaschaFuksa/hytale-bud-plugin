package com.bud.llm.messages.world;

import com.bud.llm.AbstractLLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.BudMessage;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.llm.messages.prompt.WorldMessage;
import com.bud.llm.messages.prompt.ZoneMessage;
import com.bud.npc.BudInstance;
import com.bud.reaction.world.time.Mood;

public class LLMWorldMessageCreation extends AbstractLLMMessageCreation {

    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMWorldContext worldContext)) {
            throw new IllegalArgumentException("Context must be of type LLMWorldContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        WorldMessage template = manager.getWorldInfoTemplate();

        ZoneMessage zoneMessage = worldContext.getZoneInfo(manager);
        String zoneInfo = zoneMessage != null ? zoneMessage.getZone() : "Unknown Zone";
        String biomeInfo = zoneMessage != null ? worldContext.getBiomeInfo(zoneMessage) : "Unknown Biome";
        String timeInfo = worldContext.getTimeInfo(manager.getTimeMessage());
        String budInfo = npcMessage.getCharacteristics();
        String environmentInfo = template.getEnvironmentInfo().formatted(zoneInfo, biomeInfo, timeInfo);
        String weatherInfo = worldContext.getWeatherInfo();
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
