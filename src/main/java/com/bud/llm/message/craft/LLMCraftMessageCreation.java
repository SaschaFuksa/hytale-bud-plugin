package com.bud.llm.message.craft;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.world.time.Mood;

/**
 * Creates LLM prompts for crafting events.
 * Uses the crafted item information and bud personality craft views.
 */
public class LLMCraftMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMCraftContext craftContext)) {
            throw new IllegalArgumentException("Context must be of type LLMCraftContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();
        LLMPromptManager manager = LLMPromptManager.getInstance();

        // Build crafting information
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

        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(budInstance.getCurrentMood().getDisplayName().toLowerCase()));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = craftingInfo + "\n"
                + manager.getSystemPrompt("final");

        return new Prompt(systemPrompt, message);
    }
}
