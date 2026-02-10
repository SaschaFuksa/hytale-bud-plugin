package com.bud.llm.message.mood;

import com.bud.llm.message.IPromptContext;

public record LLMMoodContext() implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        return null;
    }

    public static LLMMoodContext from() {
        return new LLMMoodContext();
    }

}
