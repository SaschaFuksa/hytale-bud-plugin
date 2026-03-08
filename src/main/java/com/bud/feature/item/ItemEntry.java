package com.bud.feature.item;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record ItemEntry(@Nonnull String itemName, @Nonnull ItemInteraction interaction,
        @Nonnull BudComponent budComponent)
        implements IQueueEntry {

    public String getCollectInformation() {
        return "Your Buddy collected following item: " + this.itemName;
    }

    @Override
    public int getPriority() {
        if (itemName.contains("gem")) {
            return 3;
        } else if (itemName.contains("ingot")) {
            return 2;
        } else if (itemName.contains("ore")) {
            return 1;
        }
        return 0;
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return itemName;
    }

}
