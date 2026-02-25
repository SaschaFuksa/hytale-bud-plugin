package com.bud.llm.interaction;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.profiles.IBudProfile;
import com.bud.core.profiles.mappings.BudProfileMapper;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.prompt.IPromptContext;

public record LLMInteractionEntry(@Nonnull AbstractLLMMessageCreation llmMessageCreation,
        @Nonnull IPromptContext promptContext,
        @Nonnull BudComponent budComponent) {

    @Nonnull
    public UUID getPlayerId() {
        return budComponent.getPlayerRef().getUuid();
    }

    @Nonnull
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
    }

}
