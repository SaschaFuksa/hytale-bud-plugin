package com.bud.feature.work;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
