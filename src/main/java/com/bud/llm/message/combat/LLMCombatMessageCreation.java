package com.bud.llm.message.combat;

import com.bud.llm.message.ILLMMessageCreation;
import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.CombatMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;

public class LLMCombatMessageCreation implements ILLMMessageCreation {

        @Override
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

                String systemPrompt = manager.getSystemPrompt("combat") + "\n"
                                + manager.getSystemPrompt("default") + "\n" + budInfo + "\n"
                                + combatView;
                String message = combatInfo + "\n" + entityInfo + "\n" + manager.getSystemPrompt("final");
                return new Prompt(systemPrompt, message);
        }

}
