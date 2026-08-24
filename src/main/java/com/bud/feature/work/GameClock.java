package com.bud.feature.work;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class GameClock {

    private GameClock() {
    }

    @Nullable
    public static Instant now(@Nonnull World world) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        WorldTimeResource timeResource = (WorldTimeResource) entityStore
                .getResource(WorldTimeResource.getResourceType());
        return timeResource.getGameTime();
    }

}
