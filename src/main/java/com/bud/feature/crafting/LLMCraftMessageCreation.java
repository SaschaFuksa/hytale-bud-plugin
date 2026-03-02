package com.bud.feature.crafting;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMCraftMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMCraftMessageCreation INSTANCE = new LLMCraftMessageCreation();

    private LLMCraftMessageCreation() {
    }

    @Nonnull
    public static LLMCraftMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMCraftContext craftContext)) {
            throw new IllegalArgumentException("Context must be of type LLMCraftContext");
        }
        BudMessage npcMessage = craftContext.getBudProfile().getBudMessage();
        LLMPromptManager manager = LLMPromptManager.getInstance();

        String craftingInfo = craftContext.getCraftingInformation();

        String budInfo = npcMessage.getCharacteristics();
        String craftView = npcMessage.getPersonalCraftView();
        if (craftView == null) {
            craftView = npcMessage.getPersonalItemView();
        }

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("craft")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n")
                .append(craftView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(craftingInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!craftContext.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            craftContext.getBudComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof LLMCraftContext craftContext)) {
            throw new IllegalArgumentException("Context must be of type LLMCraftContext");
        }
        String interactionKey = switch (craftContext.craftEntry().interaction()) {
            case CRAFTED -> "craftViewCrafted";
            case USED -> "craftViewUsed";
        };
        String message = craftContext.getBudProfile().getBudMessage()
                .getFallback(interactionKey);
        return new Prompt(message, message);
    }
}
