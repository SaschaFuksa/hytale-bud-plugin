package com.bud.llm.messages.favoriteday;

import com.bud.llm.messages.IPromptContext;

public record LLMFavoriteDayContext() implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        return null;
    }

    public static LLMFavoriteDayContext from() {
        return new LLMFavoriteDayContext();
    }

}
