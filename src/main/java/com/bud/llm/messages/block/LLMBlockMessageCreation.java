package com.bud.llm.messages.block;

import com.bud.llm.AbstractLLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.BudMessage;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.block.BlockInteraction;
import com.bud.reaction.world.time.Mood;

public class LLMBlockMessageCreation extends AbstractLLMMessageCreation {

    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMBlockContext blockContext)) {
            throw new IllegalArgumentException("Context must be of type LLMBlockContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();

        String playerName = blockContext.player().getUsername();
        String blockName = blockContext.blockName();
        BlockInteraction interaction = blockContext.interaction();

        // Simple context message for the LLM
        String interactionInfo = String.format("Your friend %s just %s a block: %s.", playerName,
                interaction.name().toLowerCase(), blockName);

        String budInfo = npcMessage.getCharacteristics();
        String personalView = npcMessage.getPersonalBlockView();
        if (personalView == null)
            personalView = npcMessage.getPersonalWorldView(); // Fallback

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("block")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n")
                .append(personalView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(interactionInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(budInstance.getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();
        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createLLMPrompt(IPromptContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Prompt createFallbackPrompt(IPromptContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
