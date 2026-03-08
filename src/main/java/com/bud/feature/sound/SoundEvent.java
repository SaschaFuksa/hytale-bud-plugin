package com.bud.feature.sound;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record SoundEvent(@Nonnull Ref<EntityStore> ref, @Nonnull String soundEventID) implements IEvent<Void> {

    public static void dispatch(@Nonnull Ref<EntityStore> ref, @Nonnull String soundEventID) {
        IEventDispatcher<SoundEvent, SoundEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(SoundEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new SoundEvent(ref, soundEventID));
        }
    }
}
