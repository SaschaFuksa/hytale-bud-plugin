package com.bud.interaction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudSoundInteraction {
    private static final String SOUND_EVENT_HINT = "Trork";
    private static final int SOUND_EVENT_LOG_LIMIT = 50;

    public void playSound(World world, NPCEntity bud, String soundEventID) {
        try {
            final int soundEventIndex = getSoundEventIndex(soundEventID);

            if (SOUND_EVENT_HINT != null) {
                logSoundEventSuggestions(SOUND_EVENT_HINT);
            }

            if (soundEventIndex == 0) {
                System.out.println("[BUD] No sound to play for sound event id: " + soundEventID);
                return;
            }

            world.execute(() -> {
                if (bud != null) {
                    Ref<EntityStore> budRef = bud.getReference();
                    if (budRef != null) {
                        Store<EntityStore> store = budRef.getStore();
                        if (store.isInThread()) {
                            TransformComponent transformComponent = store.getComponent(budRef, TransformComponent.getComponentType());
                            Vector3d position = transformComponent != null ? transformComponent.getPosition() : null;
                            if (position != null) {
                                SoundUtil.playSoundEvent3d(budRef, soundEventIndex, position.getX(), position.getY(), position.getZ(), false, store);
                            }
                        }
                    }
                }
            });

            System.out.println("[BUD] Played sound for state change.");
        } catch (Exception e) {
            System.out.println("[BUD] Failed to play sound: " + e.getMessage());
        }
    }

    private int getSoundEventIndex(String soundEventId) {
        if (soundEventId == null || soundEventId.isBlank()) {
            return 0;
        }

        int soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (soundEventIndex == Integer.MIN_VALUE) {
            System.out.println("[BUD] Invalid sound event id: " + soundEventId);
            return 0;
        }

        return soundEventIndex;
    }

    private void logSoundEventSuggestions(String contains) {
        try {
            String filter = contains == null ? "" : contains.trim();
            String upperFilter = filter.toUpperCase();
            StringBuilder builder = new StringBuilder("[BUD] Available SoundEvent IDs");
            if (!upperFilter.isBlank()) {
                builder.append(" containing '").append(filter).append("'");
            }
            builder.append(": ");

            int count = 0;
            boolean first = true;
            for (String key : SoundEvent.getAssetMap().getAssetMap().keySet()) {
                String upperKey = key == null ? "" : key.toUpperCase();
                if (upperFilter.isBlank() || upperKey.contains(upperFilter)) {
                    if (!first) {
                        builder.append(", ");
                    }
                    builder.append(key);
                    first = false;
                    count++;
                    if (count >= SOUND_EVENT_LOG_LIMIT) {
                        builder.append(" ...");
                        break;
                    }
                }
            }

            if (count == 0) {
                builder.append("<none>");
            }

            System.out.println(builder);
        } catch (Exception e) {
            System.out.println("[BUD] Failed to list SoundEvent IDs: " + e.getMessage());
        }
    }
    
}
