package com.bud.feature.teleport;

import com.bud.core.components.BudComponent;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMTeleportContext(BudComponent budComponent, IBudProfile budProfile) implements IPromptContext {

    public static LLMTeleportContext from(BudComponent budComponent, IBudProfile budProfile) {
        return new LLMTeleportContext(budComponent, budProfile);
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
