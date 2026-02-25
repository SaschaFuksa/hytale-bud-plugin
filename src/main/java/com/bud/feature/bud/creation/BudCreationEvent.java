package com.bud.feature.bud.creation;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.types.BudType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record BudCreationEvent(@Nonnull Ref<EntityStore> playerRef, @Nonnull Set<BudType> budTypes)
        implements IEvent<Void> {

    public static void dispatch(@Nonnull Ref<EntityStore> playerRef, @Nonnull Set<BudType> budTypes) {
        IEventDispatcher<BudCreationEvent, BudCreationEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(BudCreationEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new BudCreationEvent(playerRef, budTypes));
        }
    }

}
