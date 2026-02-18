package com.bud.events;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record CreateBudEvent(@Nonnull Ref<EntityStore> playerRef, @Nonnull String budType) implements IEvent<Void> {

    public static void dispatch(@Nonnull Ref<EntityStore> playerRef, @Nonnull String budType) {
        IEventDispatcher<CreateBudEvent, CreateBudEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(CreateBudEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new CreateBudEvent(playerRef, budType));
        }
    }

}
