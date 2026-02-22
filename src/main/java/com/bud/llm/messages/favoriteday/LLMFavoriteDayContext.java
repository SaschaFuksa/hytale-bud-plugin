package com.bud.llm.messages.favoriteday;

import com.bud.components.BudComponent;
import com.bud.llm.messages.IPromptContext;
import com.bud.profile.IBudProfile;

public record LLMFavoriteDayContext() implements IPromptContext {

    public static LLMFavoriteDayContext from() {
        return new LLMFavoriteDayContext();
    }

    @Override
    public BudComponent getBudComponent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudComponent'");
    }

    @Override
    public IBudProfile getBudProfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudProfile'");
    }

}
