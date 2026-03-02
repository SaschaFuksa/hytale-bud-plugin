package com.bud.feature.teleport;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMTeleportContext(@Nonnull BudComponent budComponent) implements IPromptContext {

    @Nonnull
    public static LLMTeleportContext from(@Nonnull BudComponent budComponent) {
        return new LLMTeleportContext(budComponent);
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
