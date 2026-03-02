package com.bud.feature.queue;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
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
    default IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(getBudComponent().getBudType());
    }

}
