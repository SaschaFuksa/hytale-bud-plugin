package com.bud.queue.teleport;

import java.util.Set;

import javax.annotation.Nonnull;

import org.jspecify.annotations.NonNull;

import com.bud.components.BudComponent;
import com.bud.components.PlayerBudComponent;
import com.bud.profile.BudType;
import com.bud.queue.ICacheEntry;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TeleportEntry(@NonNull PlayerBudComponent playerBudComponent, @NonNull BudComponent budComponent,
        @NonNull Set<BudType> budTypes,
        @Nonnull Store<EntityStore> store) implements ICacheEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

}
