package com.bud.feature.item;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMItemMessageCreation extends AbstractLLMMessageCreation {

        @Nonnull
        private static final LLMItemMessageCreation INSTANCE = new LLMItemMessageCreation();

        private LLMItemMessageCreation() {
        }

        @Nonnull
        public static LLMItemMessageCreation getInstance() {
                return INSTANCE;
        }

        @Override
        protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
                if (!(context instanceof LLMItemContext itemContext)) {
                        throw new IllegalArgumentException("Context must be of type LLMItemContext");
                }
                BudMessage npcMessage = itemContext.getBudProfile().getBudMessage();
                LLMPromptManager manager = LLMPromptManager.getInstance();

                String collectInformation = itemContext.getCollectInformation();
                ItemMessage itemPromptMessage = manager.getItemPromptMessage();
                final String itemInformation = (itemContext.itemEntry().interaction().equals(ItemInteraction.INVENTORY)
                                ? itemPromptMessage.getInventory()
                                : itemPromptMessage.getPickup())
                                .entrySet().stream()
                                .filter(entry -> itemContext.itemEntry().itemName().toLowerCase()
                                                .contains(entry.getKey().toLowerCase()))
                                .map(entry -> "\n" + entry.getValue())
                                .findFirst()
                                .orElse("");

                String budInfo = npcMessage.getCharacteristics();
                String itemView = npcMessage.getPersonalItemView();

                StringBuilder systemPromptBuilder = new StringBuilder();
                systemPromptBuilder.append(manager.getSystemPrompt("item")).append("\n")
                                .append(manager.getSystemPrompt("default")).append("\n")
                                .append(budInfo).append("\n")
                                .append(itemView);

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append(collectInformation).append("\n")
                                .append(itemInformation).append("\n")
                                .append(manager.getSystemPrompt("final"));

                if (!itemContext.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
                        systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt(
                                                        itemContext.getBudComponent().getCurrentMood().getDisplayName()
                                                                        .toLowerCase()));
                        messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
                }

                String systemPrompt = systemPromptBuilder.toString();
                String message = messageBuilder.toString();

                return new Prompt(systemPrompt, message);
        }

        @Override
        protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
                if (!(context instanceof LLMItemContext itemContext)) {
                        throw new IllegalArgumentException("Context must be of type LLMItemContext");
                }
                String message = itemContext.getBudProfile().getBudMessage()
                                .getFallback("itemView");
                return new Prompt(message, message);
        }

}
