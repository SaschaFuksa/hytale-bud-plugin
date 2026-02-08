package com.bud.llm.message.state;

import com.bud.llm.message.creation.IPromptContext;

public record LLMStateContext(String state) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        if ("state".equals(contextId)) {
            return this.state;
        }
        return null;
    }

    public static LLMStateContext from(String state) {
        return new LLMStateContext(state);
    }

    public String getStateInformation() {
        return "The current state of you is: " + state;
    }

}
