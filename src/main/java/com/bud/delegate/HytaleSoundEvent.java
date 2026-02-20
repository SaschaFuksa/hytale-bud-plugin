package com.bud.delegate;

import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;

public class HytaleSoundEvent {

    public static int getSoundIndex(String soundEventId) {
        if (soundEventId == null || soundEventId.isBlank()) {
            return 0;
        }
        return SoundEvent.getAssetMap().getIndex(soundEventId);
    }

}
