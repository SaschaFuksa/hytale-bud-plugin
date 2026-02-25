package com.bud.feature.queue.creation;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.feature.profile.BudType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record BudCreationEntry(@Nonnull Ref<EntityStore> playerRef, @Nonnull BudType budType) {

    @Nonnull
    @SuppressWarnings("null")
    public Set<BudType> budTypes() {
        return Set.of(this.budType);
    }

}
