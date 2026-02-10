package com.bud.llm.message.block;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.block.BlockInteraction;
import com.bud.reaction.world.time.Mood;

public class LLMBlockMessageCreation implements ILLMMessageCreation {

    @Override
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

        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(budInstance.getCurrentMood().getDisplayName().toLowerCase()));
        }
        String systemPrompt = systemPromptBuilder.toString();

        String message = interactionInfo + "\n" + manager.getSystemPrompt("final");
        return new Prompt(systemPrompt, message);
    }
}
