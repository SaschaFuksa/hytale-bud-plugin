package com.bud.queue.teleport;

import java.util.Set;

import javax.annotation.Nonnull;

import org.jspecify.annotations.NonNull;

import com.bud.components.PlayerBudComponent;
import com.bud.profile.BudType;
import com.bud.queue.IQueueEntry;
import com.bud.queue.InteractionEntry;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TeleportEntry(@NonNull PlayerBudComponent playerBudComponent,
        @NonNull Set<BudType> budTypes, @Nonnull Store<EntityStore> store, @NonNull InteractionEntry interactionEntry)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public InteractionEntry getInteractionEntry() {
        return interactionEntry;
    }

}
