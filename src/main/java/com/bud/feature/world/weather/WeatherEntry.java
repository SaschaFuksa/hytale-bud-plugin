package com.bud.feature.world.weather;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record WeatherEntry(@Nonnull String weatherName, @Nonnull BudComponent budComponent) implements IQueueEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return weatherName;
    }

}
