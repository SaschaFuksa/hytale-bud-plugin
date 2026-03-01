package com.bud.feature.block;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMBlockContext(String blockName, BlockInteraction interaction, BudComponent budComponent)
        implements IPromptContext {

    @Nonnull
    public static LLMBlockContext from(String blockName, BlockInteraction interaction, BudComponent budComponent) {
        return new LLMBlockContext(blockName, interaction, budComponent);
    }

    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
    }

}
