package com.bud.feature.combat;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record PlayerAttackEntityEvent(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> playerRef,
        @Nonnull Ref<EntityStore> targetRef)
        implements IEvent<Void> {

    public static void dispatch(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Ref<EntityStore> targetRef) {
        IEventDispatcher<PlayerAttackEntityEvent, PlayerAttackEntityEvent> dispatcher = HytaleServer.get()
                .getEventBus()
                .dispatchFor(PlayerAttackEntityEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new PlayerAttackEntityEvent(store, playerRef, targetRef));
        }
    }

}