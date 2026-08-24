package com.bud.feature.work.mining;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class OreGrowthBlock implements Component<ChunkStore> {

    public static final int STAGE_HOLE = 0;

    public static final int STAGE_READY = 1;

    public static final int STAGE_NODE_STONE = 2;

    public static final int KIND_RANDOM = 0;

    public static final int KIND_NODE_ARM = 1;

    public static final int KIND_NODE_CENTER = 2;

    private static ComponentType<ChunkStore, OreGrowthBlock> TYPE;

    private int growthStage = STAGE_HOLE;

    private int nodeKind = KIND_RANDOM;

    @Nullable
    private String oreBlockId;

    @Nullable
    private Instant nextGrowthAt;

    private transient boolean promotionPending;

    @Nonnull
    public static final BuilderCodec<OreGrowthBlock> CODEC = BuilderCodec
            .builder(OreGrowthBlock.class, OreGrowthBlock::new)
            .append(new KeyedCodec<>("GrowthStage", Codec.INTEGER),
                    (entity, value) -> entity.growthStage = value != null ? value : STAGE_HOLE,
                    entity -> entity.growthStage)
            .add()
            .append(new KeyedCodec<>("NextGrowthAt", Codec.INSTANT),
                    (entity, value) -> entity.nextGrowthAt = value,
                    entity -> entity.nextGrowthAt)
            .add()
            .append(new KeyedCodec<>("NodeKind", Codec.INTEGER),
                    (entity, value) -> entity.nodeKind = value != null ? value : KIND_RANDOM,
                    entity -> entity.nodeKind)
            .add()
            .append(new KeyedCodec<>("OreBlockId", Codec.STRING),
                    (entity, value) -> entity.oreBlockId = value,
                    entity -> entity.oreBlockId)
            .add()
            .build();

    public static void setComponentType(ComponentType<ChunkStore, OreGrowthBlock> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<ChunkStore, OreGrowthBlock> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getChunkStoreRegistry().registerComponent(
                    OreGrowthBlock.class,
                    "OreGrowthBlock",
                    OreGrowthBlock.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = growthStage;
    }

    public boolean isReady() {
        return growthStage == STAGE_READY;
    }

    public int getNodeKind() {
        return nodeKind;
    }

    public void setNodeKind(int nodeKind) {
        this.nodeKind = nodeKind;
    }

    @Nullable
    public String getOreBlockId() {
        return oreBlockId;
    }

    public void setOreBlockId(@Nullable String oreBlockId) {
        this.oreBlockId = oreBlockId;
    }

    @Nullable
    public Instant getNextGrowthAt() {
        return nextGrowthAt;
    }

    public void setNextGrowthAt(@Nullable Instant nextGrowthAt) {
        this.nextGrowthAt = nextGrowthAt;
    }

    public boolean isPromotionPending() {
        return promotionPending;
    }

    public void setPromotionPending(boolean promotionPending) {
        this.promotionPending = promotionPending;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<ChunkStore> clone() {
        try {
            return (OreGrowthBlock) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
