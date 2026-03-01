package com.bud.feature.teleport;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMTeleportContext(BudComponent budComponent) implements IPromptContext {

    @Nonnull
    public static LLMTeleportContext from(BudComponent budComponent) {
        return new LLMTeleportContext(budComponent);
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
