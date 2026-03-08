package com.bud.feature.queue.teleport;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TeleportEntry(@Nonnull BudComponent budComponent, @Nonnull Store<EntityStore> store)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return "teleport";
    }

}
