package com.bud.llm.message.mood;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;

public class LLMMoodMessageCreation implements ILLMMessageCreation {

        @Override
        public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
                BudMessage npcMessage = budInstance.getData().getBudMessage();

                LLMPromptManager manager = LLMPromptManager.getInstance();

                String budInfo = npcMessage.getCharacteristics();
                String favoriteDayInfo = npcMessage.getFavoriteDayView();

                StringBuilder systemPromptBuilder = new StringBuilder();
                systemPromptBuilder.append(manager.getSystemPrompt("favoriteDay")).append("\n")
                                .append(manager.getSystemPrompt("default")).append("\n")
                                .append(budInfo).append("\n");

                String systemPrompt = systemPromptBuilder.toString();
                String message = favoriteDayInfo + "\n" + manager.getSystemPrompt("final");
                return new Prompt(systemPrompt, message);
        }

}
