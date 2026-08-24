package com.bud.feature.work.lumbering;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.bud.feature.work.BlockDrops;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

public final class WorkstationWoodUtil {

    private static final String WOOD_BLOCK_PREFIX = "Wood_";

    private static final String TRUNK_BLOCK_MARKER = "_Trunk";

    public static final int MAX_CONNECTED_BLOCKS = 256;

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
        return blockId != null && blockId.startsWith(WOOD_BLOCK_PREFIX);
    }

    public static boolean hasTrunkBlock(@Nonnull World world, @Nonnull List<Vector3i> connectedWoodBlocks) {
        for (Vector3i position : connectedWoodBlocks) {
            BlockType blockType = world.getBlockType(position.x, position.y, position.z);
            String blockId = blockType != null ? blockType.getId() : null;
            if (blockId != null && blockId.contains(TRUNK_BLOCK_MARKER)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public static List<Vector3i> connectedWoodBlocks(@Nonnull World world, @Nonnull Vector3i start, int maxBlocks) {
        List<Vector3i> result = new ArrayList<>();
        if (!isWoodBlock(world.getBlockType(start.x, start.y, start.z))) {
            return result;
        }
        Deque<Vector3i> queue = new ArrayDeque<>();
        Set<Vector3i> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Vector3i current = queue.poll();
            result.add(current);
            for (int[] offset : NEIGHBOR_OFFSETS) {
                Vector3i neighbor = new Vector3i(current.x + offset[0], current.y + offset[1], current.z + offset[2]);
                if (visited.add(neighbor) && isWoodBlock(world.getBlockType(neighbor.x, neighbor.y, neighbor.z))) {
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    public static final String LIFE_ESSENCE_ITEM_ID = "Ingredient_Life_Essence";

    public static final int MAX_LIFE_ESSENCE_QUANTITY = 50;

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
