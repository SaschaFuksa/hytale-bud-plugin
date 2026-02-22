package com.bud.llm.messages.item;

import com.bud.llm.messages.ILLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.llm.messages.prompt.BudMessage;
import com.bud.llm.messages.prompt.ItemPromptMessage;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.reaction.item.ItemInteraction;
import com.bud.reaction.world.time.Mood;

public class LLMItemMessageCreation implements ILLMMessageCreation {

        @Override
        public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
                if (!(context instanceof LLMItemContext itemContext)) {
                        throw new IllegalArgumentException("Context must be of type LLMItemContext");
                }
                BudMessage npcMessage = budInstance.getData().getBudMessage();

                LLMPromptManager manager = LLMPromptManager.getInstance();

                String collectInformation = itemContext.getCollectInformation();
                ItemPromptMessage itemPromptMessage = manager.getItemPromptMessage();
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
