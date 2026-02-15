package com.bud.llm.message.item;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.world.time.Mood;

public class LLMItemMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
        if (!(context instanceof LLMItemContext itemContext)) {
            throw new IllegalArgumentException("Context must be of type LLMItemContext");
        }
        BudMessage npcMessage = budInstance.getData().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();

        String itemInformation = LLMItemContext.getItemInformation(itemContext.itemName());

        String budInfo = npcMessage.getCharacteristics();
        String itemView = npcMessage.getPersonalItemView();

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("item")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n")
                .append(itemView);
        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            budInstance.getCurrentMood().getDisplayName().toLowerCase()));
        }
        String systemPrompt = systemPromptBuilder.toString();
        String message = itemInformation + "\n" + manager.getSystemPrompt("final");
        return new Prompt(systemPrompt, message);
    }

}
