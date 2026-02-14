package com.bud.llm.message.favoriteday;

import com.bud.llm.message.IPromptContext;

public record LLMFavoriteDayContext() implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        return null;
    }

    public static LLMFavoriteDayContext from() {
        return new LLMFavoriteDayContext();
    }

}
