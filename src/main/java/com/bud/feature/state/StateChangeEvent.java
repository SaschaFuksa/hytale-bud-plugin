package com.bud.feature.state;

import javax.annotation.Nonnull;

import com.bud.core.types.BudState;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.event.IEventDispatcher;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public record StateChangeEvent(@Nonnull NPCEntity bud, @Nonnull PlayerRef owner, @Nonnull BudState newState)
        implements IEvent<Void> {

    public static void dispatch(@Nonnull NPCEntity bud, @Nonnull PlayerRef owner, @Nonnull BudState newState) {
        IEventDispatcher<StateChangeEvent, StateChangeEvent> dispatcher = HytaleServer.get().getEventBus()
                .dispatchFor(StateChangeEvent.class);
        if (dispatcher.hasListener()) {
            dispatcher.dispatch(new StateChangeEvent(bud, owner, newState));
        }
    }

}
