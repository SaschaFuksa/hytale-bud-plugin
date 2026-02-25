package com.bud.feature.state;

import com.bud.core.components.BudComponent;
import com.bud.llm.prompt.IPromptContext;
import com.bud.feature.profile.IBudProfile;

public record LLMStateContext(BudComponent budComponent, IBudProfile budProfile) implements IPromptContext {

    public static LLMStateContext from(BudComponent budComponent, IBudProfile budProfile) {
        return new LLMStateContext(budComponent, budProfile);
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
        return budProfile;
    }

}
