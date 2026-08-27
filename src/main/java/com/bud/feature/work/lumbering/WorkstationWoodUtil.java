package com.bud.feature.work.lumbering;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.bud.feature.work.BlockDrops;
import com.bud.feature.work.FieldCandidates;
import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class WorkstationWoodUtil {

    private static final String WOOD_BLOCK_PREFIX = "Wood_";

    private static final String TRUNK_BLOCK_MARKER = "_Trunk";

    private static final String[] TREE_PART_SUFFIXES = {
            "_Trunk_Full", "_Trunk", "_Branch_Long", "_Branch_Short", "_Branch_Corner", "_Roots"
    };

    public static final int MAX_CONNECTED_BLOCKS = 4096;

    private static final int[][] NEIGHBOR_OFFSETS = {
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 }
    };

    private static final int PHYSICS_WAKE_HORIZONTAL_MARGIN = 1;

    private static final int PHYSICS_WAKE_HEIGHT_ABOVE = 5;

    private WorkstationWoodUtil() {
    }

    public static boolean isWoodBlock(@Nullable BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        String blockId = blockType.getId();
        if (blockId == null || !blockId.startsWith(WOOD_BLOCK_PREFIX)) {
            return false;
        }
        for (String suffix : TREE_PART_SUFFIXES) {
            if (blockId.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True for a {@code Wood_}-prefixed block that is a player-built construction (fence, planks, beam, roof, wall,
     * ...) rather than an actual tree part - see {@link #isWoodBlock} for the tree-part whitelist this excludes.
     */
    public static boolean isProtectedWoodConstruction(@Nullable BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        String blockId = blockType.getId();
        return blockId != null && blockId.startsWith(WOOD_BLOCK_PREFIX) && !isWoodBlock(blockType);
    }

    public static boolean isTreeMature(@Nonnull World world, @Nonnull List<Vector3i> connectedWoodBlocks) {
        ComponentType<ChunkStore, FarmingBlock> farmingType = FarmingBlock.getComponentType();
        if (farmingType == null) {
            return true;
        }
        for (Vector3i position : connectedWoodBlocks) {
            if (!FieldCandidates.isChunkLoaded(world, position.x, position.z)) {
                continue;
            }
            Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);
            if (holder != null && holder.getComponent(farmingType) != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasTrunkBlock(@Nonnull World world, @Nonnull List<Vector3i> connectedWoodBlocks) {
        for (Vector3i position : connectedWoodBlocks) {
            BlockType blockType = FieldCandidates.getBlockType(world, position.x, position.y, position.z);
            String blockId = blockType != null ? blockType.getId() : null;
            if (blockId != null && blockId.contains(TRUNK_BLOCK_MARKER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Result of a connected-wood-block scan. When {@code truncated} is true, {@code blocks} is always empty - the
     * tree exceeded {@link #MAX_CONNECTED_BLOCKS} and the caller should abort rather than fell it partially.
     */
    public record WoodBlockScan(@Nonnull List<Vector3i> blocks, boolean truncated) {
    }

    /**
     * Flood-fills wood blocks connected to {@code start}, restricted to the field's own planting slot: a neighbor is
     * only followed if its horizontally nearest entry in {@code plantSpotColumns} is the same one {@code start}
     * itself belongs to. This keeps the scan from crossing into a neighboring tree when two trees' canopies touch.
     * Pass an empty list to disable the restriction (falls back to unrestricted flood-fill).
     */
    @Nonnull
    public static WoodBlockScan connectedWoodBlocks(@Nonnull World world, @Nonnull Vector3i start, int maxBlocks,
            @Nonnull List<Vector3i> plantSpotColumns) {
        if (!isWoodBlock(FieldCandidates.getBlockType(world, start.x, start.y, start.z))) {
            return new WoodBlockScan(new ArrayList<>(), false);
        }
        Vector3i ownerColumn = nearestColumn(plantSpotColumns, start.x, start.z);
        List<Vector3i> result = new ArrayList<>();
        Deque<Vector3i> queue = new ArrayDeque<>();
        Set<Vector3i> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Vector3i current = queue.poll();
            result.add(current);
            for (int[] offset : NEIGHBOR_OFFSETS) {
                Vector3i neighbor = new Vector3i(current.x + offset[0], current.y + offset[1], current.z + offset[2]);
                if (visited.add(neighbor)
                        && isWoodBlock(FieldCandidates.getBlockType(world, neighbor.x, neighbor.y, neighbor.z))
                        && belongsToOwnerColumn(plantSpotColumns, ownerColumn, neighbor.x, neighbor.z)) {
                    queue.add(neighbor);
                }
            }
        }
        boolean truncated = result.size() >= maxBlocks && !queue.isEmpty();
        return new WoodBlockScan(truncated ? new ArrayList<>() : result, truncated);
    }

    private static boolean belongsToOwnerColumn(@Nonnull List<Vector3i> plantSpotColumns,
            @Nullable Vector3i ownerColumn, int x, int z) {
        if (plantSpotColumns.isEmpty() || ownerColumn == null) {
            return true;
        }
        Vector3i nearest = nearestColumn(plantSpotColumns, x, z);
        return nearest != null && nearest.x == ownerColumn.x && nearest.z == ownerColumn.z;
    }

    @Nullable
    private static Vector3i nearestColumn(@Nonnull List<Vector3i> columns, int x, int z) {
        Vector3i nearest = null;
        long bestDistanceSquared = Long.MAX_VALUE;
        for (Vector3i column : columns) {
            long dx = column.x - x;
            long dz = column.z - z;
            long distanceSquared = dx * dx + dz * dz;
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                nearest = column;
            }
        }
        return nearest;
    }

    /**
     * Removes wood-part fragments left dangling at the boundary of a just-felled tree - branch tips that crossed
     * into a neighboring tree's planting slot and were therefore excluded from the fell itself, but structurally
     * belonged only to the tree that's now gone. A fragment is left alone if it's still connected to a trunk that
     * has real ground contact (i.e. it's part of a neighboring tree that's still standing) or if the scan hits
     * {@code maxBlocks} before it can tell (conservatively assumed grounded rather than risking a real tree).
     * Drops for removed fragments are added to {@code output} the same way the main fell's drops are.
     */
    public static void removeOrphanedWoodFragments(@Nonnull World world, @Nonnull Set<Vector3i> removed,
            @Nullable ItemContainer output, int maxBlocks) {
        Set<Vector3i> boundary = new HashSet<>();
        for (Vector3i position : removed) {
            for (int[] offset : NEIGHBOR_OFFSETS) {
                Vector3i neighbor = new Vector3i(position.x + offset[0], position.y + offset[1], position.z + offset[2]);
                if (!removed.contains(neighbor)
                        && isWoodBlock(FieldCandidates.getBlockType(world, neighbor.x, neighbor.y, neighbor.z))) {
                    boundary.add(neighbor);
                }
            }
        }
        Set<Vector3i> resolved = new HashSet<>();
        for (Vector3i start : boundary) {
            if (resolved.contains(start)) {
                continue;
            }
            WoodFragment fragment = scanFragment(world, Objects.requireNonNull(start), maxBlocks);
            resolved.addAll(fragment.blocks());
            if (fragment.groundedTrunk()) {
                continue;
            }
            List<ItemStack> drops = collectFellingDrops(world, new ArrayList<>(fragment.blocks()));
            for (Vector3i orphan : fragment.blocks()) {
                world.setBlock(orphan.x, orphan.y, orphan.z, BlockType.EMPTY_KEY);
            }
            if (output != null) {
                output.addItemStacks(drops, false, false, false);
            }
        }
    }

    private record WoodFragment(@Nonnull Set<Vector3i> blocks, boolean groundedTrunk) {
    }

    @Nonnull
    private static WoodFragment scanFragment(@Nonnull World world, @Nonnull Vector3i start, int maxBlocks) {
        Set<Vector3i> blocks = new HashSet<>();
        Set<Vector3i> visited = new HashSet<>();
        Deque<Vector3i> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        boolean groundedTrunk = false;
        while (!queue.isEmpty() && blocks.size() < maxBlocks) {
            Vector3i current = Objects.requireNonNull(queue.poll());
            blocks.add(current);
            if (isGroundedTrunk(world, current)) {
                groundedTrunk = true;
            }
            for (int[] offset : NEIGHBOR_OFFSETS) {
                Vector3i neighbor = new Vector3i(current.x + offset[0], current.y + offset[1], current.z + offset[2]);
                if (visited.add(neighbor)
                        && isWoodBlock(FieldCandidates.getBlockType(world, neighbor.x, neighbor.y, neighbor.z))) {
                    queue.add(neighbor);
                }
            }
        }
        boolean truncated = blocks.size() >= maxBlocks && !queue.isEmpty();
        return new WoodFragment(blocks, groundedTrunk || truncated);
    }

    private static boolean isGroundedTrunk(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType blockType = FieldCandidates.getBlockType(world, position.x, position.y, position.z);
        String blockId = blockType != null ? blockType.getId() : null;
        if (blockId == null || !blockId.contains(TRUNK_BLOCK_MARKER)) {
            return false;
        }
        BlockType below = FieldCandidates.getBlockType(world, position.x, position.y - 1, position.z);
        return below != null && below != BlockType.EMPTY && !isWoodBlock(below);
    }

    public static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";

    public static final int MAX_LIFE_ESSENCE_QUANTITY = 30;

    @Nonnull
    public static List<ItemStack> collectFellingDrops(@Nonnull World world,
            @Nonnull List<Vector3i> connectedWoodBlocks) {
        List<ItemStack> drops = new ArrayList<>();
        for (Vector3i position : connectedWoodBlocks) {
            drops.addAll(resolveDrops(world.getBlockType(position.x, position.y, position.z)));
        }
        if (!connectedWoodBlocks.isEmpty()) {
            int lifeEssenceQuantity = Math.min(connectedWoodBlocks.size(), MAX_LIFE_ESSENCE_QUANTITY);
            drops.add(new ItemStack(LIFE_ESSENCE_ITEM_ID, lifeEssenceQuantity));
        }
        return drops;
    }

    public static void wakeSurroundingBlocksForPhysicsRecheck(@Nonnull World world,
            @Nonnull List<Vector3i> connectedWoodBlocks) {
        if (connectedWoodBlocks.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Vector3i position : connectedWoodBlocks) {
            minX = Math.min(minX, position.x);
            maxX = Math.max(maxX, position.x);
            minY = Math.min(minY, position.y);
            maxY = Math.max(maxY, position.y);
            minZ = Math.min(minZ, position.z);
            maxZ = Math.max(maxZ, position.z);
        }

        int wakeMinX = minX - PHYSICS_WAKE_HORIZONTAL_MARGIN;
        int wakeMaxX = maxX + PHYSICS_WAKE_HORIZONTAL_MARGIN;
        int wakeMinZ = minZ - PHYSICS_WAKE_HORIZONTAL_MARGIN;
        int wakeMaxZ = maxZ + PHYSICS_WAKE_HORIZONTAL_MARGIN;
        int wakeMaxY = maxY + PHYSICS_WAKE_HEIGHT_ABOVE;

        for (int x = wakeMinX; x <= wakeMaxX; x++) {
            for (int z = wakeMinZ; z <= wakeMaxZ; z++) {
                WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                if (chunk == null) {
                    continue;
                }
                for (int y = minY; y <= wakeMaxY; y++) {
                    chunk.setTicking(x, y, z, true);
                }
            }
        }
    }

    @Nonnull
    public static List<ItemStack> resolveDrops(@Nullable BlockType blockType) {
        return BlockDrops.resolveBreakingDrops(blockType);
    }

}
