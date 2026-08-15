package com.bud.feature.work.reaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.types.Mood;
import com.bud.core.types.WorkType;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMWorkMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMWorkMessageCreation INSTANCE = new LLMWorkMessageCreation();

    private LLMWorkMessageCreation() {
    }

    @Nonnull
    public static LLMWorkMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof WorkEntry workEntry)) {
            throw new IllegalArgumentException("Context must be of type WorkEntry");
        }
        LLMPromptManager manager = LLMPromptManager.getInstance();
        BudMessage budMessage = workEntry.getBudProfile().getBudMessage();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt(workEntry.kind().getSystemPromptKey())).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budMessage.getCharacteristics()).append("\n")
                .append(budMessage.getPersonalWorkView());

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(describeWorkType(workEntry.workType()));
        if (workEntry.kind() == WorkReactionKind.OUTPUT_FULL) {
            messageBuilder.append("\n").append(describeBlockedItem(workEntry.blockedItemId()));
        }
        messageBuilder.append("\n").append(manager.getSystemPrompt("final"));

        if (!workEntry.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            workEntry.budComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        return new Prompt(systemPromptBuilder.toString(), messageBuilder.toString());
    }

    @Nonnull
    private static String describeWorkType(@Nullable WorkType workType) {
        if (workType == null) {
            return "You do not have a specific work task assigned right now.";
        }
        return "Your current work task is: " + workType.name().toLowerCase() + ".";
    }

    @Nonnull
    private static String describeBlockedItem(@Nullable String blockedItemId) {
        if (blockedItemId == null) {
            return "You cannot harvest right now because your output storage is full.";
        }
        String itemName = blockedItemId.replace("_", " ");
        return "Your output storage is full of " + itemName + " and you cannot harvest more of it right now.";
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof WorkEntry workEntry)) {
            throw new IllegalArgumentException("Context must be of type WorkEntry");
        }
        String message = workEntry.getBudProfile().getBudMessage().getFallback(workEntry.kind().getFallbackKey());
        return new Prompt(message, message);
    }
}
