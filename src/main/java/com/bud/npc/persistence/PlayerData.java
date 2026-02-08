package com.bud.npc.persistence;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.codec.codecs.set.SetCodec;

public class PlayerData implements Component<EntityStore> {

    private Set<UUID> buds;

    public static final BuilderCodec<PlayerData> CODEC = BuilderCodec.builder(
            PlayerData.class,
            PlayerData::new)
            .append(new KeyedCodec<>("BudUUIDs", new SetCodec<>(Codec.UUID_STRING, HashSet::new, false)),
                    (data, value) -> data.buds = value,
                    data -> data.buds)
            .add()
            .build();

    public PlayerData() {
        this.buds = new HashSet<>();
    }

    public PlayerData(PlayerData clone) {
        this.buds = clone.buds;
    }

    public void resetBuds() {
        this.buds.clear();
    }

    public void add(UUID npcUuid) {
        this.buds.add(npcUuid);
    }

    public Set<UUID> getBuds() {
        return this.buds;
    }

    public void remove(UUID npcUuid) {
        if (this.buds.contains(npcUuid)) {
            this.buds.remove(npcUuid);
        }
    }

    @Override
    public Component<EntityStore> clone() {
        return new PlayerData(this);
    }
}