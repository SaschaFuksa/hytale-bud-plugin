package com.bud.feature.state;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMStateContext(@Nonnull BudComponent budComponent) implements IPromptContext {

    public static LLMStateContext from(@Nonnull BudComponent budComponent) {
        return new LLMStateContext(budComponent);
    }

    public String getStateInformation() {
        return "The current state of you is: " + budComponent.getCurrentState().getStateName();
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
