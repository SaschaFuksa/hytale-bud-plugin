package com.bud.llm;

import com.bud.config.LLMConfig;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;

public abstract class AbstractLLMMessageCreation {

    public Prompt createPrompt(IPromptContext context) {
        if (LLMConfig.getInstance().isEnableLLM()) {
            return createLLMPrompt(context);
        } else {
            return createFallbackPrompt(context);
        }
    }

    protected abstract Prompt createLLMPrompt(IPromptContext context);

    protected abstract Prompt createFallbackPrompt(IPromptContext context);

}
