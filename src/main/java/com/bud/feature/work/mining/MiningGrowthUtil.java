package com.bud.feature.work.mining;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.bud.core.config.WorkConfig;
import com.bud.feature.work.FieldCandidates;
import com.bud.feature.work.GameClock;
import com.bud.feature.work.WorkRecipeConfig;
import com.bud.feature.work.WorldBlockEntities;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class MiningGrowthUtil {

    public static final String HOLE_BLOCK_ID = "Mining_Growth_Hole";

    public static final String READY_BLOCK_ID = "Mining_Growth_Ready";

    public static final String NODE_STONE_BLOCK_ID = "Mining_Node_Stone";

    private MiningGrowthUtil() {
    }

    public static void startGrowth(@Nonnull World world, @Nonnull Vector3i position, int nodeKind,
            @Nullable String oreBlockId) {
        OreGrowthBlock growth = placeGrowthBlock(world, position, HOLE_BLOCK_ID);
        if (growth == null) {
            return;
        }
        growth.setGrowthStage(OreGrowthBlock.STAGE_HOLE);
        growth.setNodeKind(nodeKind);
        growth.setOreBlockId(oreBlockId);
        scheduleNextGrowth(world, growth);
    }

    @Nullable
    private static OreGrowthBlock placeGrowthBlock(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull String blockId) {
        world.setBlock(position.x, position.y, position.z, blockId);
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
        if (chunk == null) {
            return null;
        }
        Ref<ChunkStore> ref = WorldBlockEntities.ensureOrFetch(chunk, position.x, position.y, position.z);
        if (ref == null || !ref.isValid()) {
            return null;
        }
        ComponentType<ChunkStore, OreGrowthBlock> type = OreGrowthBlock.getComponentType();
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        OreGrowthBlock growth = chunkStore.getComponent(ref, type);
        if (growth == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Mining block " + blockId + " at " + position
                    + " carries no OreGrowthBlock component - growth stalls there.");
            return null;
        }
        chunk.setTicking(position.x, position.y, position.z, true);
        return growth;
    }

    private static void scheduleNextGrowth(@Nonnull World world, @Nonnull OreGrowthBlock growth) {
        Instant now = GameClock.now(world);
        growth.setNextGrowthAt(now != null ? now.plusSeconds(rollGrowthGameSeconds()) : null);
    }

    private static long rollGrowthGameSeconds() {
        WorkConfig config = WorkConfig.getInstance();
        long min = config.getMiningGrowthGameSecondsMin();
        long max = config.getMiningGrowthGameSecondsMax();
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    public static void advanceIfDue(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull OreGrowthBlock growth) {
        Instant nextGrowthAt = growth.getNextGrowthAt();
        int stage = growth.getGrowthStage();
        if (growth.isPromotionPending() || nextGrowthAt == null
                || (stage != OreGrowthBlock.STAGE_HOLE && stage != OreGrowthBlock.STAGE_NODE_STONE)) {
            return;
        }
        Instant now = GameClock.now(world);
        if (now == null || now.isBefore(nextGrowthAt)) {
            return;
        }
        growth.setPromotionPending(true);
        world.execute(() -> advance(world, position));
    }

    private static void advance(@Nonnull World world, @Nonnull Vector3i position) {
        OreGrowthBlock pending = getGrowth(world, position);
        if (pending == null) {
            return;
        }
        int stage = pending.getGrowthStage();
        int nodeKind = pending.getNodeKind();
        String oreBlockId = pending.getOreBlockId();
        if (stage == OreGrowthBlock.STAGE_HOLE) {
            if (nodeKind == OreGrowthBlock.KIND_RANDOM) {
                growIntoRandomStone(world, position);
            } else {
                growIntoNodeStone(world, position, nodeKind, oreBlockId);
            }
            return;
        }
        if (stage == OreGrowthBlock.STAGE_NODE_STONE) {
            growIntoOre(world, position, oreBlockId);
        }
    }

    private static void growIntoRandomStone(@Nonnull World world, @Nonnull Vector3i position) {
        OreGrowthBlock growth = placeGrowthBlock(world, position, READY_BLOCK_ID);
        if (growth == null) {
            return;
        }
        growth.setGrowthStage(OreGrowthBlock.STAGE_READY);
        growth.setNodeKind(OreGrowthBlock.KIND_RANDOM);
        growth.setNextGrowthAt(null);
    }

    private static void growIntoNodeStone(@Nonnull World world, @Nonnull Vector3i position, int nodeKind,
            @Nullable String oreBlockId) {
        OreGrowthBlock growth = placeGrowthBlock(world, position, NODE_STONE_BLOCK_ID);
        if (growth == null) {
            return;
        }
        growth.setGrowthStage(OreGrowthBlock.STAGE_NODE_STONE);
        growth.setNodeKind(nodeKind);
        growth.setOreBlockId(oreBlockId);
        scheduleNextGrowth(world, growth);
        if (nodeKind == OreGrowthBlock.KIND_NODE_CENTER) {
            placeNodeCap(world, position, oreBlockId);
        }
    }

    private static void placeNodeCap(@Nonnull World world, @Nonnull Vector3i centerPosition,
            @Nullable String oreBlockId) {
        Vector3i capPosition = new Vector3i(centerPosition.x, centerPosition.y + 1, centerPosition.z);
        BlockType existing = world.getBlockType(capPosition.x, capPosition.y, capPosition.z);
        if (existing != null && existing.getMaterial() != BlockMaterial.Empty) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Mining node cap position " + capPosition
                    + " is blocked - the node stays flat.");
            return;
        }
        OreGrowthBlock cap = placeGrowthBlock(world, capPosition, NODE_STONE_BLOCK_ID);
        if (cap == null) {
            return;
        }
        cap.setGrowthStage(OreGrowthBlock.STAGE_NODE_STONE);
        cap.setNodeKind(OreGrowthBlock.KIND_NODE_ARM);
        cap.setOreBlockId(oreBlockId);
        scheduleNextGrowth(world, cap);
    }

    private static void growIntoOre(@Nonnull World world, @Nonnull Vector3i position,
            @Nullable String oreBlockId) {
        if (oreBlockId == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Mining node at " + position
                    + " has no target ore block recorded - it stays a plain node stone.");
            return;
        }
        world.setBlock(position.x, position.y, position.z, oreBlockId);
    }

    public static void clear(@Nonnull World world, @Nonnull Vector3i position) {
        if (standsOnMiningBlock(world, position)) {
            world.setBlock(position.x, position.y, position.z, BlockType.EMPTY_KEY);
            return;
        }
        String refillBlock = WorkRecipeConfig.getInstance().getDigRefillBlock();
        world.setBlock(position.x, position.y, position.z,
                refillBlock != null ? refillBlock : BlockType.EMPTY_KEY);
    }

    private static boolean standsOnMiningBlock(@Nonnull World world, @Nonnull Vector3i position) {
        Vector3i below = new Vector3i(position.x, position.y - 1, position.z);
        if (getGrowth(world, below) != null) {
            return true;
        }
        BlockType belowType = world.getBlockType(below.x, below.y, below.z);
        String belowId = belowType != null ? belowType.getId() : null;
        return belowId != null && WorkRecipeConfig.getInstance().isOreBlock(belowId);
    }

    @Nullable
    public static OreGrowthBlock getGrowth(@Nonnull World world, @Nonnull Vector3i position) {
        if (!FieldCandidates.isChunkLoaded(world, position.x, position.z)) {
            return null;
        }
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);
        if (holder == null) {
            return null;
        }
        return holder.getComponent(OreGrowthBlock.getComponentType());
    }

}
