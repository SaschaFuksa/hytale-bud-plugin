package com.bud.feature.world;

import java.util.Optional;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class WorldResolver {

    private WorldResolver() {
    }

    public static Optional<World> resolveStrict(PlayerRef playerRef) {
        if (playerRef == null) {
            return Optional.empty();
        }
        return resolveStrict(playerRef.getReference());
    }

    public static Optional<World> resolveStrict(Ref<EntityStore> entityRef) {
        if (entityRef == null) {
            return Optional.empty();
        }
        Store<EntityStore> store = entityRef.getStore();
        return resolveStrict(store);
    }

    public static Optional<World> resolveStrict(Store<EntityStore> store) {
        if (store == null || store.getExternalData() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.getExternalData().getWorld());
    }

    public static Optional<World> resolveDefaultWorld() {
        return Optional.ofNullable(WorldInformationUtil.getDefaultWorld());
    }

    public static Optional<World> resolveWithDefaultFallback(PlayerRef playerRef) {
        Optional<World> strict = resolveStrict(playerRef);
        if (strict.isPresent()) {
            return strict;
        }
        return resolveDefaultWorld();
    }
}