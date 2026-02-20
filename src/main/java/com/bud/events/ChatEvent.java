package com.bud.events;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;

public record ChatEvent(@Nonnull BudComponent budComponent, @Nonnull String message) implements IEvent<Void> {

    public static void dispatch(@Nonnull BudComponent budComponent, @Nonnull String message) {
        IEventDispatcher<ChatEvent, ChatEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(ChatEvent.class);

        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new ChatEvent(budComponent, message));
        }
    }

}
