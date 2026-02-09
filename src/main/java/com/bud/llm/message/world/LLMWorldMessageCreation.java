package com.bud.llm.message.world;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.llm.message.prompt.WorldMessage;
import com.bud.llm.message.prompt.ZoneMessage;
import com.bud.npc.BudInstance;

public class LLMWorldMessageCreation implements ILLMMessageCreation {

    @Override
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

        String systemPrompt = manager.getSystemPrompt("world") + "\n"
                + manager.getSystemPrompt("default") + "\n" + budInfo + "\n" + personalView;
        String message = environmentInfo + "\n" + weatherInfo + "\n" + manager.getSystemPrompt("final");
        return new Prompt(systemPrompt, message);
    }

}
