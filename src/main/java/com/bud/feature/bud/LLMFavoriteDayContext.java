package com.bud.feature.bud;

import com.bud.core.components.BudComponent;
import com.bud.llm.prompt.IPromptContext;
import com.bud.feature.profile.IBudProfile;

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
