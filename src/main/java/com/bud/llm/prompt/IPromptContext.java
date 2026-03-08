package com.bud.llm.prompt;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.llm.profiles.IBudProfile;

public interface IPromptContext {

    @Nonnull
    BudComponent getBudComponent();

    @Nonnull
    IBudProfile getBudProfile();

}
