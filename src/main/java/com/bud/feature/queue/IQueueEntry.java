package com.bud.feature.queue;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.registry.BudDefinition;
import com.bud.core.registry.BudRegistry;
import com.bud.llm.prompt.IPromptContext;

public interface IQueueEntry extends IPromptContext {

    int getPriority();

    @Nonnull
    String getEntryName();

    @Nonnull
    @Override
    BudComponent getBudComponent();

    @Nonnull
    @Override
    default BudDefinition getBudProfile() {
        return BudRegistry.getInstance().get(getBudComponent().getBudId());
    }

}
