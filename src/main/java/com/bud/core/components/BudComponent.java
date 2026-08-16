package com.bud.core.components;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.types.BudState;
import com.bud.core.types.Mood;
import com.bud.core.types.WorkType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, BudComponent> TYPE;

    @Nonnull
    private BudState currentState = BudState.PET_DEFENSIVE;

    private Mood currentMood = Mood.DEFAULT;

    @Nonnull
    private String budId = "veri";

    private NPCEntity bud;

    private PlayerRef playerRef;

    @Nullable
    private Vector3d workstationAnchor;

    @Nullable
    private Vector3d workTarget;

    @Nullable
    private WorkType workType;

    @Nullable
    private String pendingCropBlockType;

    @Nullable
    private Vector3i pendingFellBlockPosition;

    private float workCooldownSecondsRemaining;

    public BudComponent() {
    }

    @Nonnull
    public static BudComponent create(@Nonnull NPCEntity bud, @Nonnull String budId, @Nonnull PlayerRef playerRef) {
        BudComponent component = new BudComponent();
        component.bud = bud;
        component.playerRef = playerRef;
        component.budId = budId;
        return component;
    }

    @Nonnull
    public static final BuilderCodec<BudComponent> CODEC = BuilderCodec.builder(BudComponent.class, BudComponent::new)
            .build();

    public static void setComponentType(ComponentType<EntityStore, BudComponent> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<EntityStore, BudComponent> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getEntityStoreRegistry().registerComponent(
                    BudComponent.class,
                    "BudComponent",
                    BudComponent.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    public void setCurrentState(@Nonnull BudState state) {
        this.currentState = state;
    }

    @Nonnull
    public BudState getCurrentState() {
        return currentState;
    }

    @Nonnull
    public String getBudId() {
        return budId;
    }

    @Nonnull
    public NPCEntity getBud() {
        if (bud == null) {
            throw new IllegalStateException("NPCEntity cannot be null in BudComponent");
        }
        return bud;
    }

    public void setBud(@Nonnull NPCEntity bud) {
        this.bud = bud;
    }

    @Nonnull
    public PlayerRef getPlayerRef() {
        if (playerRef == null) {
            throw new IllegalStateException("PlayerRef cannot be null in BudComponent");
        }
        return playerRef;
    }

    public void setCurrentMood(Mood mood) {
        this.currentMood = mood;
    }

    public Mood getCurrentMood() {
        return currentMood;
    }

    @Nullable
    public Vector3d getWorkstationAnchor() {
        return workstationAnchor;
    }

    public void setWorkstationAnchor(@Nullable Vector3d workstationAnchor) {
        this.workstationAnchor = workstationAnchor;
    }

    @Nullable
    public Vector3d getWorkTarget() {
        return workTarget;
    }

    public void setWorkTarget(@Nullable Vector3d workTarget) {
        this.workTarget = workTarget;
    }

    @Nullable
    public WorkType getWorkType() {
        return workType;
    }

    public void setWorkType(@Nullable WorkType workType) {
        this.workType = workType;
    }

    @Nullable
    public String getPendingCropBlockType() {
        return pendingCropBlockType;
    }

    public void setPendingCropBlockType(@Nullable String pendingCropBlockType) {
        this.pendingCropBlockType = pendingCropBlockType;
    }

    @Nullable
    public Vector3i getPendingFellBlockPosition() {
        return pendingFellBlockPosition;
    }

    public void setPendingFellBlockPosition(@Nullable Vector3i pendingFellBlockPosition) {
        this.pendingFellBlockPosition = pendingFellBlockPosition;
    }

    public float getWorkCooldownSecondsRemaining() {
        return workCooldownSecondsRemaining;
    }

    public void setWorkCooldownSecondsRemaining(float workCooldownSecondsRemaining) {
        this.workCooldownSecondsRemaining = workCooldownSecondsRemaining;
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
