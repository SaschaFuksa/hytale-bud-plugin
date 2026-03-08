package com.bud.feature.teleport;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TeleportEvent(@Nonnull Store<EntityStore> store, @Nonnull BudComponent budComponent,
        boolean shouldSendReaction)
        implements IEvent<Void> {

    public static void dispatch(@Nonnull Store<EntityStore> store, @Nonnull BudComponent budComponent) {
        dispatch(store, budComponent, true);
    }

    public static void dispatch(@Nonnull Store<EntityStore> store, @Nonnull BudComponent budComponent,
            boolean shouldSendReaction) {
        IEventDispatcher<TeleportEvent, TeleportEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(TeleportEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new TeleportEvent(store, budComponent, shouldSendReaction));
        }
    }
}
