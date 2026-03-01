package com.bud.feature.combat;

import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMCombatContext(@Nonnull OpponentEntry opponentEntry, @Nonnull String combatContext)
        implements IPromptContext {

    @Nonnull
    public static LLMCombatContext from(@Nonnull OpponentEntry entry) {
        String cleanName = entry.entityName().replace("_Bud", "").replace("_", " ");
        String combatContext = switch (entry.state()) {
            case ATTACKED -> "Your Buddy has attacked " + cleanName + ".";
            case WAS_ATTACKED -> "Your Buddy was attacked by " + cleanName + ".";
        };
        return new LLMCombatContext(entry, combatContext);
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
        String lowerTarget = this.opponentEntry.entityName().toLowerCase();

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
        if (template == null)
            return null;

        if (category.equalsIgnoreCase("playerallies")) {
            return template.getAllyInfoTemplate().formatted(category, catInfo, specInfo);
        }
        return template.getTargetInfoTemplate().formatted(category, catInfo + " " + specInfo);
    }

    @Override
    public BudComponent getBudComponent() {
        return opponentEntry.getBudComponent();
    }

    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(opponentEntry.getBudComponent().getBudType());
    }

}
