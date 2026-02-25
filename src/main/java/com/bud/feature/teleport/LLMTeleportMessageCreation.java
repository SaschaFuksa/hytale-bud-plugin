package com.bud.feature.teleport;

import javax.annotation.Nonnull;

import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;
import com.bud.core.types.BudMessage;
import com.bud.feature.reaction.world.time.Mood;

public class LLMTeleportMessageCreation extends AbstractLLMMessageCreation {

    private static final LLMTeleportMessageCreation INSTANCE = new LLMTeleportMessageCreation();

    private LLMTeleportMessageCreation() {
    }

    @Nonnull
    public static LLMTeleportMessageCreation getInstance() {
        if (INSTANCE == null) {
            return new LLMTeleportMessageCreation();
        }
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(IPromptContext context) {
        if (!(context instanceof LLMTeleportContext teleportContext)) {
            throw new IllegalArgumentException("Context must be of type LLMTeleportContext");
        }
        BudMessage npcMessage = teleportContext.budProfile().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String teleportInfo = npcMessage.getTeleportInformation();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("teleport")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(teleportInfo).append("\n");

        if (!teleportContext.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            teleportContext.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }
        messageBuilder.append("\n").append(manager.getSystemPrompt("final"));

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(IPromptContext context) {
        if (!(context instanceof LLMTeleportContext teleportContext)) {
            throw new IllegalArgumentException("Context must be of type LLMTeleportContext");
        }
        String message = teleportContext.budProfile().getBudMessage()
                .getFallback("teleport");
        return new Prompt(message, message);
    }

}
