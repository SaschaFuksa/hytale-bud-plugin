package com.bud.feature.chat;

import javax.annotation.Nonnull;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public record ChatEvent(@Nonnull PlayerRef playerRef, @Nonnull String message)
        implements IEvent<Void> {

    public static void dispatch(@Nonnull PlayerRef playerRef, @Nonnull String message) {
        IEventDispatcher<ChatEvent, ChatEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(ChatEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new ChatEvent(playerRef, message));
        }
    }

}
