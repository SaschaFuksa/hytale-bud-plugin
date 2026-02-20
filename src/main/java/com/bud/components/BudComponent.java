package com.bud.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, BudComponent> TYPE;

    private String currentStateName = "";

    private final NPCEntity bud;

    private final PlayerRef playerRef;

    public BudComponent() {
        this.bud = null;
        this.playerRef = null;
    }

    public BudComponent(NPCEntity bud, PlayerRef playerRef, String currentStateNameName) {
        this.bud = bud;
        this.playerRef = playerRef;
        this.currentStateName = currentStateNameName;
    }

    public static final BuilderCodec<BudComponent> CODEC = BuilderCodec.builder(BudComponent.class, BudComponent::new)
            // .append(new KeyedCodec<>("CurrentStateName", Codec.STRING),
            // BudComponent::setCurrentStateName,
            // BudComponent::getCurrentStateName)
            // .add()
            .build();

    public static void setComponentType(ComponentType<EntityStore, BudComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, BudComponent> getComponentType() {
        return TYPE;
    }

    public void setCurrentStateName(String state) {
        this.currentStateName = state;
    }

    public String getCurrentStateName() {
        return currentStateName;
    }

    public NPCEntity getBud() {
        return bud;
    }

    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<EntityStore> clone() {
        try {
            return (BudComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
