package com.bud.feature.chat.player;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMPlayerChatMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMPlayerChatMessageCreation INSTANCE = new LLMPlayerChatMessageCreation();

    private LLMPlayerChatMessageCreation() {
    }

    @Nonnull
    public static LLMPlayerChatMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof PlayerChatEntry chatEntry)) {
            throw new IllegalArgumentException("Context must be of type PlayerChatEntry");
        }

        LLMPromptManager manager = LLMPromptManager.getInstance();
        BudMessage budMessage = chatEntry.getBudProfile().getBudMessage();

        String playerChatPrompt = manager.getSystemPrompt("playerChat");

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(playerChatPrompt).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budMessage.getCharacteristics());

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(chatEntry.getPlayerChatInformation()).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!chatEntry.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            chatEntry.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        return new Prompt(systemPromptBuilder.toString(), messageBuilder.toString());
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof PlayerChatEntry chatEntry)) {
            throw new IllegalArgumentException("Context must be of type PlayerChatEntry");
        }

        String message = chatEntry.getBudProfile().getBudMessage().getFallback("playerChatView");
        return new Prompt(message, message);
    }
}
