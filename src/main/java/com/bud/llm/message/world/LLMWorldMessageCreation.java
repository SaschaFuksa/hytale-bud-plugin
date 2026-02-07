package com.bud.llm.message.world;

import com.bud.llm.message.creation.ILLMMessageCreation;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.llm.message.prompt.WorldMessage;
import com.bud.llm.message.prompt.ZoneMessage;

public class LLMWorldMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudMessage npcMessage) {
        if (!(context instanceof LLMWorldContext worldContext)) {
            throw new IllegalArgumentException("Context must be of type LLMWorldContext");
        }
        LLMPromptManager manager = LLMPromptManager.getInstance();
        WorldMessage template = manager.getWorldInfoTemplate();

        ZoneMessage zoneMessage = worldContext.getZoneInfo(manager);
        String zoneInfo = zoneMessage != null ? zoneMessage.getZone() : "Unknown Zone";
        String biomeInfo = zoneMessage != null ? worldContext.getBiomeInfo(zoneMessage) : "Unknown Biome";
        String timeInfo = worldContext.getTimeInfo(manager.getTimeMessage());
        String budInfo = npcMessage.getSystemPrompt();
        String environmentInfo = template.getEnvironmentInfo().formatted(zoneInfo, biomeInfo, timeInfo);
        String personalView = npcMessage.getPersonalWorldView();

        String systemPrompt = manager.getSystemPrompt("world") + "\n"
                + manager.getSystemPrompt("default") + "\n" + budInfo;
        String message = environmentInfo + "\n" + personalView;
        return new Prompt(systemPrompt, message);
    }

}
