package com.bud.feature.block;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMBlockMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMBlockMessageCreation INSTANCE = new LLMBlockMessageCreation();

    private LLMBlockMessageCreation() {
    }

    @Nonnull
    public static LLMBlockMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMBlockContext blockContext)) {
            throw new IllegalArgumentException("Context must be of type LLMBlockContext");
        }
        BudMessage npcMessage = blockContext.getBudProfile().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();

        String playerName = blockContext.blockEntry().budComponent().getPlayerRef().getUsername();
        String blockName = blockContext.blockEntry().blockName();
        BlockInteraction interaction = blockContext.blockEntry().interaction();

        String interactionInfo = String.format("Your friend %s just %s a block: %s.", playerName,
                interaction.name().toLowerCase(), blockName);

        String budInfo = npcMessage.getCharacteristics();
        String personalView = npcMessage.getPersonalBlockView();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("block")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n")
                .append(personalView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(interactionInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!blockContext.blockEntry().budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            blockContext.blockEntry().budComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();
        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMBlockContext blockContext)) {
            throw new IllegalArgumentException("Context must be of type LLMBlockContext");
        }
        String message = blockContext.getBudProfile().getBudMessage()
                .getFallback("block");
        return new Prompt(message, message);
    }
}
