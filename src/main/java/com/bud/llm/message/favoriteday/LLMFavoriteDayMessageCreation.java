package com.bud.llm.message.favoriteday;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.world.time.Mood;

public class LLMFavoriteDayMessageCreation implements ILLMMessageCreation {

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

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append(favoriteDayInfo).append("\n")
                                .append(manager.getSystemPrompt("final"));

                if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
                        systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt(
                                                        budInstance.getCurrentMood().getDisplayName().toLowerCase()));
                        messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
                }

                String systemPrompt = systemPromptBuilder.toString();
                String message = messageBuilder.toString();

                return new Prompt(systemPrompt, message);
        }

}
