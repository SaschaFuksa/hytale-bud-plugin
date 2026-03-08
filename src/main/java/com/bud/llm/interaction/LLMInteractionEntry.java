package com.bud.llm.interaction;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.prompt.IPromptContext;

public record LLMInteractionEntry(@Nonnull AbstractLLMMessageCreation llmMessageCreation,
        @Nonnull IPromptContext promptContext) {

    @Nonnull
    public BudComponent getBudComponent() {
        return promptContext.getBudComponent();
    }

}
