package com.bud.feature.bud;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMFavoriteDayContext(@Nonnull BudComponent budComponent) implements IPromptContext {

    @Nonnull
    public static LLMFavoriteDayContext from(@Nonnull BudComponent budComponent) {
        return new LLMFavoriteDayContext(budComponent);
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Nonnull
    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
    }

}
