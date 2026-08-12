package com.bud.feature.work;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.components.BudComponent;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class WorkstationBlockEntity implements Component<ChunkStore> {

    private static ComponentType<ChunkStore, WorkstationBlockEntity> TYPE;

    @Nonnull
    private static final Codec<WorkRole> WORK_ROLE_CODEC = new EnumCodec<>(WorkRole.class);

    @Nonnull
    private WorkRole workRole = WorkRole.FARMING;

    @Nullable
    private UUID ownerPlayerId;

    // Runtime-only binding state (Phase 4), not persisted - like BudComponent's own bud/playerRef fields,
    // this is a live link re-established from the container's current contents on block (re)load rather
    // than restored from disk. See docs/bud-worker-mode-plan.md, "Bindung + Fuel-Timer (Phase 4)".
    @Nullable
    private BudComponent boundBud;

    private float fuelSecondsRemaining;

    private boolean resting;

    // Runtime-only throttle (not persisted, same reasoning as fuelSecondsRemaining/resting above) for the
    // rebind-retry in WorkstationFuelTickSystem: covers the case where the Workstation's block/chunk loads
    // (owner-online resolution fails) before the owning player has finished logging back in - see
    // docs/bud-worker-mode-plan.md, "Persistenz über Relog/Neustart".
    private float rebindRetrySecondsRemaining;

    @Nonnull
    public static final BuilderCodec<WorkstationBlockEntity> CODEC = BuilderCodec
            .builder(WorkstationBlockEntity.class, WorkstationBlockEntity::new)
            .append(new KeyedCodec<>("WorkRole", WORK_ROLE_CODEC),
                    (entity, value) -> entity.workRole = value != null ? value : WorkRole.FARMING,
                    entity -> entity.workRole)
            .add()
            .append(new KeyedCodec<>("OwnerPlayerId", Codec.UUID_BINARY),
                    (entity, value) -> entity.ownerPlayerId = value,
                    entity -> entity.ownerPlayerId)
            .add()
            .build();

    public static void setComponentType(ComponentType<ChunkStore, WorkstationBlockEntity> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<ChunkStore, WorkstationBlockEntity> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getChunkStoreRegistry().registerComponent(
                    WorkstationBlockEntity.class,
                    "WorkstationBlockEntity",
                    WorkstationBlockEntity.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    @Nonnull
    public WorkRole getWorkRole() {
        return workRole;
    }

    public void setWorkRole(@Nonnull WorkRole workRole) {
        this.workRole = workRole;
    }

    @Nullable
    public UUID getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(@Nullable UUID ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

    @Nullable
    public BudComponent getBoundBud() {
        return boundBud;
    }

    public void setBoundBud(@Nullable BudComponent boundBud) {
        this.boundBud = boundBud;
    }

    public float getFuelSecondsRemaining() {
        return fuelSecondsRemaining;
    }

    public void setFuelSecondsRemaining(float fuelSecondsRemaining) {
        this.fuelSecondsRemaining = fuelSecondsRemaining;
    }

    public boolean isResting() {
        return resting;
    }

    public void setResting(boolean resting) {
        this.resting = resting;
    }

    public float getRebindRetrySecondsRemaining() {
        return rebindRetrySecondsRemaining;
    }

    public void setRebindRetrySecondsRemaining(float rebindRetrySecondsRemaining) {
        this.rebindRetrySecondsRemaining = rebindRetrySecondsRemaining;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<ChunkStore> clone() {
        try {
            return (WorkstationBlockEntity) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
