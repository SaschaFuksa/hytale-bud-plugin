package com.bud.feature.bud;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMFavoriteDayMessageCreation extends AbstractLLMMessageCreation {

        @Override
        public Prompt createLLMPrompt(@Nonnull IPromptContext context) {
                if (!(context instanceof FavoriteDayEntry favoriteDayEntry)) {
                        throw new IllegalArgumentException("Context must be of type FavoriteDayEntry");
                }
                BudMessage npcMessage = favoriteDayEntry.getBudProfile().getBudMessage();

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

                if (!favoriteDayEntry.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
                        systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt(
                                                        favoriteDayEntry.budComponent().getCurrentMood()
                                                                        .getDisplayName()
                                                                        .toLowerCase()));
                        messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
                }

                String systemPrompt = systemPromptBuilder.toString();
                String message = messageBuilder.toString();

                return new Prompt(systemPrompt, message);
        }

        @Override
        protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
                if (!(context instanceof FavoriteDayEntry favoriteDayEntry)) {
                        throw new IllegalArgumentException("Context must be of type FavoriteDayEntry");
                }
                String message = favoriteDayEntry.getBudProfile().getBudMessage()
                                .getFallback("favoriteDayView");
                return new Prompt(message, message);
        }

}
