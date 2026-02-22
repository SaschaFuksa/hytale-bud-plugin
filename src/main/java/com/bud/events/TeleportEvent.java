package com.bud.events;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.components.PlayerBudComponent;
import com.bud.profile.BudType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TeleportEvent(@Nonnull Store<EntityStore> store, @Nonnull PlayerBudComponent playerBudComponent,
        @Nonnull Set<BudType> budTypes)
        implements IEvent<Void> {

    public static void dispatch(@Nonnull Store<EntityStore> store, @Nonnull PlayerBudComponent playerBudComponent,
            @Nonnull Set<BudType> budTypes) {
        IEventDispatcher<TeleportEvent, TeleportEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(TeleportEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new TeleportEvent(store, playerBudComponent, budTypes));
        }
    }
}
