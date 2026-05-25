package com.bud.feature.chat.conversation;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMDialogMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMDialogMessageCreation INSTANCE = new LLMDialogMessageCreation();

    private LLMDialogMessageCreation() {
    }

    @Nonnull
    public static LLMDialogMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof DialogEntry dialogEntry)) {
            throw new IllegalArgumentException("Context must be of type DialogEntry");
        }

        LLMPromptManager manager = LLMPromptManager.getInstance();
        BudMessage budMessage = dialogEntry.getBudProfile().getBudMessage();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("dialog")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budMessage.getCharacteristics());

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(dialogEntry.getDialogInformation()).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!dialogEntry.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            dialogEntry.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        return new Prompt(systemPromptBuilder.toString(), messageBuilder.toString());
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof DialogEntry dialogEntry)) {
            throw new IllegalArgumentException("Context must be of type DialogEntry");
        }

        String message = dialogEntry.getBudProfile().getBudMessage().getFallback("playerChatView");
        return new Prompt(message, message);
    }
}