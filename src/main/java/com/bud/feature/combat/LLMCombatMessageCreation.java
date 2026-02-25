package com.bud.feature.combat;

import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;
import com.bud.core.types.BudMessage;
import com.bud.feature.data.npc.BudInstance;
import com.bud.feature.reaction.world.time.Mood;

public class LLMCombatMessageCreation extends AbstractLLMMessageCreation {

        public Prompt createPrompt(IPromptContext context, BudInstance budInstance) {
                if (!(context instanceof LLMCombatContext combatContext)) {
                        throw new IllegalArgumentException("Context must be of type LLMCombatContext");
                }
                BudMessage npcMessage = budInstance.getData().getBudMessage();

                LLMPromptManager manager = LLMPromptManager.getInstance();
                CombatMessage template = manager.getCombatInfoTemplate();

                String entityInfo = combatContext.getEntityInformation();
                String playerName = combatContext.player().getUsername();
                entityInfo = entityInfo.replace("$player$", playerName);

                String contextInfo = combatContext.combatContext();
                String combatInfo = template.getCombatInfo().formatted(contextInfo);
                String budInfo = npcMessage.getCharacteristics();
                String combatView = npcMessage.getPersonalCombatView();

                StringBuilder systemPromptBuilder = new StringBuilder();
                systemPromptBuilder.append(manager.getSystemPrompt("combat")).append("\n")
                                .append(manager.getSystemPrompt("default")).append("\n")
                                .append(budInfo).append("\n")
                                .append(combatView);

                StringBuilder messageBuilder = new StringBuilder();
                messageBuilder.append(combatInfo).append("\n")
                                .append(entityInfo).append("\n")
                                .append(manager.getSystemPrompt("final"));

                if (!budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt("instruction"));
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt(
                                                        budInstance.getCurrentMood().getDisplayName().toLowerCase()));
                        messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
                }

                String systemPrompt = systemPromptBuilder.toString();
                String message = messageBuilder.toString();
                return new Prompt(systemPrompt, message);
        }

        @Override
        protected Prompt createLLMPrompt(IPromptContext context) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'createLLMPrompt'");
        }

        @Override
        protected Prompt createFallbackPrompt(IPromptContext context) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'createFallbackPrompt'");
        }

}
