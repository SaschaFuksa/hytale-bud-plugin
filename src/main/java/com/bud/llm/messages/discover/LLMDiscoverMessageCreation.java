package com.bud.llm.messages.discover;

import com.bud.llm.messages.ILLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.BudMessage;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.llm.messages.prompt.ZoneMessage;
import com.bud.npc.BudInstance;
import com.bud.reaction.world.time.Mood;

/**
 * Creates LLM prompts for zone discovery events.
 * Uses the zone description from the existing zone YAML files
 * and matches the regionName to known biomes.
 */
public class LLMDiscoverMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMDiscoverContext discoverContext)) {
            throw new IllegalArgumentException("Context must be of type LLMDiscoverContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();
        LLMPromptManager manager = LLMPromptManager.getInstance();

        // Get zone description from existing zone YAMLs
        ZoneMessage zoneMessage = discoverContext.getZoneInfo(manager);
        String zoneDescription = (zoneMessage != null) ? zoneMessage.getZone() : "an unknown area";

        // Build discovery information
        String discoveryInfo = discoverContext.getDiscoveryInformation();

        String budInfo = npcMessage.getCharacteristics();
        String discoverView = npcMessage.getPersonalDiscoverView();
        if (discoverView == null) {
            discoverView = npcMessage.getPersonalWorldView();
        }

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("discover")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n")
                .append(discoverView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(discoveryInfo).append("\n")
                .append("Zone description: ").append(zoneDescription).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(budInstance.getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }
}
