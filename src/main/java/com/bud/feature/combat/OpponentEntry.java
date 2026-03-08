package com.bud.feature.combat;

import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.queue.IQueueEntry;

public record OpponentEntry(@Nonnull String entityName, @Nonnull CombatState state, @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    public String getCombatContext() {
        String cleanName = entityName.replace("_Bud", "").replace("_", " ");
        return switch (state) {
            case ATTACKED -> "Your Buddy has attacked " + cleanName + ".";
            case WAS_ATTACKED -> "Your Buddy was attacked by " + cleanName + ".";
        };
    }

    public String getEntityInformation() {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        EntityCategoriesMessage entityData = manager.getEntityCategories();

        if (entityData == null || entityData.getCategories() == null) {
            return null;
        }

        return findAndFormatMatch(entityData, manager.getCombatInfoTemplate());
    }

    private String findAndFormatMatch(EntityCategoriesMessage entityData, CombatMessage template) {
        String lowerTarget = this.entityName.toLowerCase();

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

    private String formatOutput(CombatMessage template, String category, String catInfo, String specInfo) {
        if (template == null) {
            return null;
        }

        if (category.equalsIgnoreCase("playerallies")) {
            return template.getAllyInfoTemplate().formatted(category, catInfo, specInfo);
        }
        return template.getTargetInfoTemplate().formatted(category, catInfo + " " + specInfo);
    }

    public boolean isAttacked() {
        return state == CombatState.ATTACKED;
    }

    public boolean wasAttacked() {
        return state == CombatState.WAS_ATTACKED;
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return entityName;
    }

}
