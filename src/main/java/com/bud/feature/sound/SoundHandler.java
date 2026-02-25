package com.bud.feature.sound;

import java.util.function.Consumer;

import com.bud.core.sound.delegate.HytaleSoundEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SoundHandler implements Consumer<SoundEvent> {

    @Override
    public void accept(SoundEvent event) {
        Ref<EntityStore> budRef = event.ref();
        if (!budRef.isValid())
            return;
        Store<EntityStore> store = budRef.getStore();
        ComponentType<EntityStore, TransformComponent> transformComponentType = TransformComponent.getComponentType();
        if (transformComponentType == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] TransformComponent type is null. Cannot play sound.");
            return;
        }
        store.getExternalData().getWorld().execute(() -> {
            TransformComponent transformComponent = store.getComponent(budRef, transformComponentType);
            Vector3d position = transformComponent.getPosition();
            SoundUtil.playSoundEvent3d(budRef, getSoundEventIndex(event.soundEventID()), position.getX(),
                    position.getY(),
                    position.getZ(), false, store);
        });
    }

    private int getSoundEventIndex(String soundEventId) {
        if (soundEventId == null || soundEventId.isBlank()) {
            return 0;
        }

        int soundEventIndex = HytaleSoundEvent.getSoundIndex(soundEventId);
        if (soundEventIndex == Integer.MIN_VALUE) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Invalid sound event id: " + soundEventId);
            return 0;
        }

        return soundEventIndex;
    }
}
