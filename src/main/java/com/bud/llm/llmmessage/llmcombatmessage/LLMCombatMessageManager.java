package com.bud.llm.llmmessage.llmcombatmessage;

import java.util.Map.Entry;

import com.bud.llm.llmmessage.BudLLMMessage;
import com.bud.llm.llmmessage.BudLLMPromptManager;
import com.bud.llm.llmmessage.CombatInfoTemplateMessage;
import com.bud.llm.llmmessage.EntityCategoriesMessage;

public class LLMCombatMessageManager {

        public static String createPrompt(String combatPrompt, BudLLMMessage npcMessage, String targetName) {
                BudLLMPromptManager manager = BudLLMPromptManager.getInstance();
                CombatInfoTemplateMessage template = manager.getCombatInfoTemplate();

                String budInfo = npcMessage.getSystemPrompt();
                String introduction = template.getIntroduction();
                String entityInformations = getEntityInformations(targetName);
                if (entityInformations == null) {
                        return null;
                }
                String combat_info = template.getCombatInfo().formatted(combatPrompt);
                String combatView = npcMessage.getPersonalCombatView();

                return budInfo + "\n" + introduction + "\n" + entityInformations + "\n" + combat_info + "\n"
                                + combatView;
        }

        private static String getEntityInformations(String targetName) {
                BudLLMPromptManager manager = BudLLMPromptManager.getInstance();
                EntityCategoriesMessage entityData = manager.getEntityCategories();
                CombatInfoTemplateMessage template = manager.getCombatInfoTemplate();

                if (entityData == null || entityData.getCategories() == null) {
                        return null;
                }

                String lowerTargetName = targetName.toLowerCase();

                for (Entry<String, EntityCategoriesMessage.CategoryData> categoryEntry : entityData
                                .getCategories()
                                .entrySet()) {
                        String categoryName = categoryEntry.getKey();
                        EntityCategoriesMessage.CategoryData data = categoryEntry.getValue();

                        if (data.getEntities() != null) {
                                for (Entry<String, String> entityEntry : data.getEntities().entrySet()) {
                                        String keyword = entityEntry.getKey().toLowerCase();
                                        if (lowerTargetName.contains(keyword)) {
                                                String categoryInfo = data.getInfo();
                                                String entitySpecificInfo = entityEntry.getValue();

                                                if (categoryName.equalsIgnoreCase("Player Allies")) {
                                                        return template.getAllyInfoTemplate().formatted(categoryName,
                                                                        categoryInfo,
                                                                        entitySpecificInfo);
                                                }
                                                return template.getTargetInfoTemplate().formatted(categoryName,
                                                                categoryInfo + " " + entitySpecificInfo);
                                        }
                                }
                        }
                }
                return null;
        }
}
