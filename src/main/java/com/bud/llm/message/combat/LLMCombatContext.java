package com.bud.llm.message.combat;

import java.util.Map.Entry;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.prompt.CombatInfoTemplateMessage;
import com.bud.llm.message.prompt.EntityCategoriesMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.system.RecentOpponentCache.OpponentEntry;

public record LLMCombatContext(String combatContext, String targetName) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        // Implement this method based on your context retrieval logic
        return switch (contextId) {
            case "combatContext" -> this.combatContext;
            case "targetName" -> this.targetName;
            default -> null;
        };
    }

    public static LLMCombatContext from(OpponentEntry entry) {
        String combatContext = switch (entry.state()) {
            case ATTACKED -> "Your Buddy attacked " + entry.roleName() + ".";
            case WAS_ATTACKED -> "Your Buddy was attacked by " + entry.roleName() + ".";
        };
        String targetName = entry.roleName().replace("_Bud", "").replace("_", " ");
        return new LLMCombatContext(combatContext, targetName);
    }

    public String getEntityInformation() {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        EntityCategoriesMessage entityData = manager.getEntityCategories();
        CombatInfoTemplateMessage template = manager.getCombatInfoTemplate();

        if (entityData == null || entityData.getCategories() == null) {
            return null;
        }

        String lowerTargetName = this.targetName.toLowerCase();

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
