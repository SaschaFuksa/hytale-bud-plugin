package com.bud.feature.combat;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMCombatMessageCreation extends AbstractLLMMessageCreation {

        @Nonnull
        private static final LLMCombatMessageCreation INSTANCE = new LLMCombatMessageCreation();

        private LLMCombatMessageCreation() {
        }

        @Nonnull
        public static LLMCombatMessageCreation getInstance() {
                return INSTANCE;
        }

        @Override
        protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
                if (!(context instanceof OpponentEntry opponentEntry)) {
                        throw new IllegalArgumentException("Context must be of type OpponentEntry");
                }
                BudMessage npcMessage = opponentEntry.getBudProfile().getBudMessage();

                LLMPromptManager manager = LLMPromptManager.getInstance();
                CombatMessage template = manager.getCombatInfoTemplate();

                String entityInfo = opponentEntry.getEntityInformation();
                String playerName = opponentEntry.getBudComponent().getPlayerRef().getUsername();
                entityInfo = entityInfo.replace("$player$", playerName);

                String contextInfo = opponentEntry.getCombatContext();
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

                if (!opponentEntry.getBudComponent().getCurrentMood().equals(Mood.DEFAULT)) {
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt("instruction"));
                        systemPromptBuilder.append("\n")
                                        .append(manager.getMoodPrompt(
                                                        opponentEntry.getBudComponent().getCurrentMood()
                                                                        .getDisplayName().toLowerCase()));
                        messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
                }

                String systemPrompt = systemPromptBuilder.toString();
                String message = messageBuilder.toString();
                return new Prompt(systemPrompt, message);
        }

        @Override
        protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
                if (!(context instanceof OpponentEntry opponentEntry)) {
                        throw new IllegalArgumentException("Context must be of type OpponentEntry");
                }
                String stateKey = switch (opponentEntry.state()) {
                        case ATTACKED -> "combatViewAttacked";
                        case WAS_ATTACKED -> "combatViewWasAttacked";
                };
                String message = opponentEntry.getBudProfile().getBudMessage()
                                .getFallback(stateKey);
                return new Prompt(message, message);
        }

}
