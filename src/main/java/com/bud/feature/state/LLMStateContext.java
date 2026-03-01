package com.bud.feature.state;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMStateContext(BudComponent budComponent) implements IPromptContext {

    public static LLMStateContext from(BudComponent budComponent) {
        return new LLMStateContext(budComponent);
    }

    public String getStateInformation() {
        return "The current state of you is: " + budComponent.getCurrentState().getStateName();
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
