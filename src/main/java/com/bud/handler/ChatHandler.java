package com.bud.handler;

import java.util.function.Consumer;

import com.bud.events.ChatEvent;
import com.bud.events.SoundEvent;
import com.hypixel.hytale.server.core.Message;

public class ChatHandler implements Consumer<ChatEvent> {

    @Override
    public void accept(ChatEvent event) {
        if (!event.budComponent().getPlayerRef().isValid())
            return;
        event.budComponent().getPlayerRef().sendMessage(Message.raw(event.message()));
        SoundEvent.dispatch(event.budComponent(), "SFX_Fox_Alerted");
    }

}
