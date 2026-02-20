package com.bud.components;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class PlayerBudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerBudComponent> TYPE;

    // private String[] loadedBuds = new String[0];
    private Set<String> buddies;

    private ConcurrentLinkedQueue<NPCEntity> buds = new ConcurrentLinkedQueue<>();

    public PlayerBudComponent() {
        this.buddies = new HashSet<>();
    }

    public PlayerBudComponent(PlayerBudComponent clone) {
        this.buds = new ConcurrentLinkedQueue<>(clone.buds);
        this.buddies = new HashSet<>(clone.buddies);
    }

    public static final BuilderCodec<PlayerBudComponent> CODEC = BuilderCodec
            .builder(
                    PlayerBudComponent.class,
                    PlayerBudComponent::new)
            .append(
                    new KeyedCodec<>("Buddies", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                    (component, value) -> component.buddies = value != null ? new HashSet<>(value) : new HashSet<>(),
                    component -> component.buddies)
            .add()
            .build();

    public static void setComponentType(ComponentType<EntityStore, PlayerBudComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, PlayerBudComponent> getComponentType() {
        return TYPE;
    }

    public synchronized void addBud(NPCEntity bud) {
        if (buds.size() >= 3) {
            return;
        }
        buds.add(bud);
        String npcTypeId = bud.getNPCTypeId();
        buddies.add(npcTypeId);
    }

    public ConcurrentLinkedQueue<NPCEntity> getBuds() {
        return buds;
    }

    public synchronized void removeBud(NPCEntity bud) {
        String npcTypeId = bud.getNPCTypeId();
        // buddies.remove(npcTypeId);
        LoggerUtil.getLogger().fine(() -> "[BUD] Removing Bud with NPC Type ID: " + npcTypeId);
        buds.remove(bud);
    }

    public Set<String> getLoadedBuds() {
        return new HashSet<>(buddies);
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<EntityStore> clone() {
        return new PlayerBudComponent(this);
    }

}
