package com.bud.events;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record SoundEvent(@Nonnull BudComponent budComponent, @Nonnull String soundEventID) implements IEvent<Void> {

    public static void dispatch(@Nonnull BudComponent budComponent, @Nonnull String soundEventID) {
        IEventDispatcher<SoundEvent, SoundEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(SoundEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new SoundEvent(budComponent, soundEventID));
        }
    }
}
