package com.bud.llm.messages.teleport;

import com.bud.components.BudComponent;
import com.bud.llm.messages.IPromptContext;
import com.bud.profile.IBudProfile;

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
