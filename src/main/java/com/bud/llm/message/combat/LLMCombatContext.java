package com.bud.llm.message.combat;

import java.util.Map.Entry;

import com.bud.combat.RecentOpponentCache.OpponentEntry;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.prompt.CombatMessage;
import com.bud.llm.message.prompt.EntityCategoriesMessage;
import com.bud.llm.message.prompt.LLMPromptManager;

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
        String cleanName = entry.roleName().replace("_Bud", "").replace("_", " ");
        String combatContext = switch (entry.state()) {
            case ATTACKED -> "Your Buddy attacked " + cleanName + ".";
            case WAS_ATTACKED -> "Your Buddy was attacked by " + cleanName + ".";
        };
        return new LLMCombatContext(combatContext, cleanName);
    }

    public String getEntityInformation() {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        EntityCategoriesMessage entityData = manager.getEntityCategories();

        if (entityData == null || entityData.getCategories() == null) {
            return null;
        }

        return findAndFormatMatch(entityData, manager.getCombatInfoTemplate());
    }

    /**
     * Iterates through categories to find a matching entity and formats the result.
     */
    private String findAndFormatMatch(EntityCategoriesMessage entityData, CombatMessage template) {
        String lowerTarget = this.targetName.toLowerCase();

        for (Entry<String, EntityCategoriesMessage.CategoryData> categoryEntry : entityData.getCategories()
                .entrySet()) {
            String categoryName = categoryEntry.getKey();
            EntityCategoriesMessage.CategoryData data = categoryEntry.getValue();

            String entitySpecificInfo = findSpecificInfo(data, lowerTarget);
            if (entitySpecificInfo != null) {
                return formatOutput(template, categoryName, data.getInfo(), entitySpecificInfo);
            }
        }
        return null;
    }

    /**
     * Checks if any entity keyword in the category matches the target name.
     */
    private String findSpecificInfo(EntityCategoriesMessage.CategoryData data, String lowerTarget) {
        if (data.getEntities() == null) {
            return null;
        }

        for (Entry<String, String> entityEntry : data.getEntities().entrySet()) {
            if (lowerTarget.contains(entityEntry.getKey().toLowerCase())) {
                return entityEntry.getValue();
            }
        }
        return null;
    }

    /**
     * Formats the final string based on the category type.
     */
    private String formatOutput(CombatMessage template, String category, String catInfo, String specInfo) {
        if (template == null)
            return null;

        if (category.equalsIgnoreCase("playerallies")) {
            return template.getAllyInfoTemplate().formatted(category, catInfo, specInfo);
        }
        return template.getTargetInfoTemplate().formatted(category, catInfo + " " + specInfo);
    }
}
