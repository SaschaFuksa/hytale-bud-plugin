package com.bud.feature.discover;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.world.env.ZoneMessage;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMDiscoverMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMDiscoverMessageCreation INSTANCE = new LLMDiscoverMessageCreation();

    private LLMDiscoverMessageCreation() {
    }

    @Nonnull
    public static LLMDiscoverMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMDiscoverContext discoverContext)) {
            throw new IllegalArgumentException("Context must be of type LLMDiscoverContext");
        }
        BudMessage npcMessage = discoverContext.getBudProfile().getBudMessage();
        LLMPromptManager manager = LLMPromptManager.getInstance();

        ZoneMessage zoneMessage = discoverContext.getZoneInfo(manager);
        String zoneDescription = (zoneMessage != null) ? zoneMessage.getZone() : "an unknown area";

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

        if (!discoverContext.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            discoverContext.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMDiscoverContext discoverContext)) {
            throw new IllegalArgumentException("Context must be of type LLMDiscoverContext");
        }
        String message = discoverContext.getBudProfile().getBudMessage()
                .getFallback("discoverView");
        return new Prompt(message, message);
    }
}
