package com.bud.components;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.bud.profile.BudType;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class PlayerBudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerBudComponent> TYPE;

    private Set<String> budIds;

    private Set<BudType> budTypes;

    private ConcurrentLinkedQueue<NPCEntity> currentBuds = new ConcurrentLinkedQueue<>();

    public PlayerBudComponent() {
        this.budIds = new HashSet<>();
        this.budTypes = new HashSet<>();
    }

    public PlayerBudComponent(PlayerBudComponent clone) {
        this.currentBuds = new ConcurrentLinkedQueue<>(clone.currentBuds);
        this.budIds = new HashSet<>(clone.budIds);
        this.budTypes = new HashSet<>(clone.budTypes);
    }

    @Nonnull
    public static final BuilderCodec<PlayerBudComponent> CODEC = BuilderCodec
            .builder(
                    PlayerBudComponent.class,
                    PlayerBudComponent::new)
            .append(
                    new KeyedCodec<>("BudIds", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                    (component, value) -> component.budIds = value != null ? new HashSet<>(value) : new HashSet<>(),
                    component -> component.budIds)
            .add()
            .append(
                    new KeyedCodec<>("BudTypes", new SetCodec<>(new EnumCodec<>(BudType.class), HashSet::new, false)),
                    (component, value) -> component.budTypes = value != null ? new HashSet<>(value) : new HashSet<>(),
                    component -> component.budTypes)
            .add()
            .build();

    public static void setComponentType(ComponentType<EntityStore, PlayerBudComponent> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<EntityStore, PlayerBudComponent> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getEntityStoreRegistry().registerComponent(
                    PlayerBudComponent.class,
                    "PlayerBuddiesComponent",
                    PlayerBudComponent.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    public synchronized void addBud(NPCEntity bud, BudType budType) {
        if (currentBuds.size() >= 3) {
            return;
        }
        LoggerUtil.getLogger().fine(() -> "[BUD] Adding Bud with NPC Type ID: " + budType.getName());
        currentBuds.add(bud);
        // String npcTypeId = budType.getName();
        // budIds.add(npcTypeId);
        budTypes.add(budType);
    }

    public ConcurrentLinkedQueue<NPCEntity> getCurrentBuds() {
        return currentBuds;
    }

    public synchronized void removeCurrentBud(NPCEntity bud, BudType budType) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Removing Bud with NPC Type ID: " + budType.getName());
        // String npcTypeId = bud.getNPCTypeId();
        // budIds.remove(npcTypeId);
        currentBuds.remove(bud);
        budTypes.remove(budType);
    }

    @Nonnull
    public Set<BudType> getBudTypes() {
        return new HashSet<>(budTypes);
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<EntityStore> clone() {
        try {
            return (PlayerBudComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
