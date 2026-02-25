package com.bud.feature.chat;

import java.util.function.Consumer;

import com.hypixel.hytale.server.core.Message;

public class ChatHandler implements Consumer<ChatEvent> {

    @Override
    public void accept(ChatEvent event) {
        if (!event.playerRef().isValid())
            return;
        event.playerRef().sendMessage(Message.raw(event.message()));
    }

}
