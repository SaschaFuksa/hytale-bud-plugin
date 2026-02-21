package com.bud.events;

import javax.annotation.Nonnull;

import org.jspecify.annotations.NonNull;

import com.bud.reaction.state.BudState;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public record StateChangeEvent(@NonNull NPCEntity bud, @Nonnull PlayerRef owner, @NonNull BudState newState)
        implements IEvent<Void> {

    public static void dispatch(@Nonnull NPCEntity bud, @Nonnull PlayerRef owner, @Nonnull BudState newState) {
        IEventDispatcher<StateChangeEvent, StateChangeEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(StateChangeEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new StateChangeEvent(bud, owner, newState));
        }
    }

}
