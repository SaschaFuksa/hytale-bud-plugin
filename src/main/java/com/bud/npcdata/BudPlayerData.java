package com.bud.npcdata;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BudPlayerData implements Component<EntityStore> {

    public static final BuilderCodec<BudPlayerData> CODEC = BuilderCodec.builder(
            BudPlayerData.class,
            BudPlayerData::new
        )
        .addField(new KeyedCodec<>("Buds", Codec.STRING),
            (data, value) -> data.deserialize(value),
            data -> data.serialize())
        .build();

    private final List<StoredBud> storedBuds = new ArrayList<>();

    public BudPlayerData() {
    }
    
    public List<StoredBud> getStoredBuds() {
        return storedBuds;
    }
    
    public void addBud(UUID uuid) {
        // Remove existing if any (update)
        removeBud(uuid);
        this.storedBuds.add(new StoredBud(uuid));
    }
    
    public void removeBud(UUID uuid) {
        this.storedBuds.removeIf(b -> b.uuid.equals(uuid));
    }
    
    private void deserialize(String raw) {
        this.storedBuds.clear();
        if (raw != null && !raw.isEmpty()) {
            String[] entries = raw.split(";");
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    try {
                        UUID u = UUID.fromString(entry);
                        this.storedBuds.add(new StoredBud(u));
                    } catch (Exception e) { }
                }
            }
        }
    }
    
    private String serialize() {
        return this.storedBuds.stream()
                .map(b -> b.uuid.toString())
                .collect(Collectors.joining(";"));
    }

    @Override
    public Component<EntityStore> clone() {
        BudPlayerData copy = new BudPlayerData();
        copy.storedBuds.addAll(this.storedBuds);
        return copy;
    }
    
    public static class StoredBud {
        public final UUID uuid;
        
        public StoredBud(UUID uuid) {
            this.uuid = uuid;
        }
    }
}
