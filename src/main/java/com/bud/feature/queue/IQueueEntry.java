package com.bud.feature.queue;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;

public interface IQueueEntry {

    int getPriority();

    @Nonnull
    BudComponent getBudComponent();

    @Nonnull
    String getEntryName();

}
