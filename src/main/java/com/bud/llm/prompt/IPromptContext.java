package com.bud.llm.prompt;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.registry.BudDefinition;

public interface IPromptContext {

    @Nonnull
    BudComponent getBudComponent();

    @Nonnull
    BudDefinition getBudProfile();

}
