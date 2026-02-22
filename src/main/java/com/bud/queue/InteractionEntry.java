package com.bud.queue;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.components.BudComponent;
import com.bud.llm.AbstractLLMMessageCreation;
import com.bud.llm.messages.IPromptContext;
import com.bud.mappings.BudProfileMapper;
import com.bud.profile.IBudProfile;

public record InteractionEntry(@Nullable AbstractLLMMessageCreation llmMessageCreation,
        @Nullable IPromptContext promptContext,
        @Nonnull BudComponent budComponent) {

    public UUID getPlayerId() {
        return budComponent.getPlayerRef().getUuid();
    }

    @Nonnull
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
    }

}
