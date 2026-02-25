package com.bud.llm.messages;

import javax.annotation.Nonnull;

import com.bud.core.config.LLMConfig;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public abstract class AbstractLLMMessageCreation {

    public Prompt createPrompt(@Nonnull IPromptContext context) {
        if (LLMConfig.getInstance().isEnableLLM()) {
            return createLLMPrompt(context);
        } else {
            return createFallbackPrompt(context);
        }
    }

    protected abstract Prompt createLLMPrompt(@Nonnull IPromptContext context);

    protected abstract Prompt createFallbackPrompt(@Nonnull IPromptContext context);

}
