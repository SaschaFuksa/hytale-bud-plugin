package com.bud.feature.work.mining;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.config.WorkConfig;
import com.bud.feature.work.FieldCandidates;
import com.bud.feature.work.WorkRecipeConfig;
import com.bud.feature.work.WorkstationSeedUtil;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;

public final class MiningFieldScan {

    private MiningFieldScan() {
    }

    public static boolean isDigCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType blockType = FieldCandidates.getBlockType(world, position.x, position.y, position.z);
        String blockId = FieldCandidates.getBlockId(blockType);
        if (blockId == null) {
            return false;
        }
        return WorkRecipeConfig.getInstance().isDiggableBlock(blockId)
                && FieldCandidates.hasFreeTopFace(world, position.x, position.y, position.z);
    }

    public static boolean isOreReadyCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        OreGrowthBlock growth = MiningGrowthUtil.getGrowth(world, position);
        return growth != null && growth.isReady();
    }

    public static boolean isGrowthBlock(@Nonnull World world, @Nonnull Vector3i position) {
        return MiningGrowthUtil.getGrowth(world, position) != null;
    }

    public static boolean isTooCloseToGrowthBlock(@Nonnull World world, @Nonnull Vector3i position) {
        int minDistance = WorkConfig.getInstance().getOreMinDistance();
        if (minDistance <= 0) {
            return false;
        }
        long minDistanceSquared = (long) minDistance * minDistance;
        for (int dx = -minDistance; dx <= minDistance; dx++) {
            for (int dz = -minDistance; dz <= minDistance; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if ((long) dx * dx + (long) dz * dz > minDistanceSquared) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    if (isGrowthBlock(world, new Vector3i(position.x + dx, position.y + dy, position.z + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int maxDigHoles() {
        return Math.max(0, WorkConfig.getInstance().getFieldRadius());
    }

    public static int mainNodeCount(int radius) {
        if (radius <= 6) {
            return 2;
        }
        return radius <= 9 ? 4 : 8;
    }

    @Nonnull
    private static List<Vector3i> nodeCenterColumns(@Nonnull Vector3d anchor, int radius) {
        int anchorX = (int) Math.floor(anchor.x);
        int anchorZ = (int) Math.floor(anchor.z);
        int distance = Math.max(1, radius - 1);
        int count = mainNodeCount(radius);
        List<Vector3i> centers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double angle = Math.PI / 2 + (2 * Math.PI * index) / count;
            Vector3i offset = nodeOffset(distance, angle, radius);
            centers.add(new Vector3i(anchorX + offset.x, 0, anchorZ + offset.z));
        }
        return centers;
    }

    @Nonnull
    private static Vector3i nodeOffset(int distance, double angle, int radius) {
        long limitSquared = (long) (radius - 1) * (radius - 1);
        for (double scale = 1.0; scale > 0.1; scale -= 0.05) {
            int offsetX = (int) Math.round(distance * scale * Math.cos(angle));
            int offsetZ = (int) Math.round(distance * scale * Math.sin(angle));
            if ((long) offsetX * offsetX + (long) offsetZ * offsetZ <= limitSquared) {
                return new Vector3i(offsetX, 0, offsetZ);
            }
        }
        return new Vector3i(0, 0, 0);
    }

    private static final int NODE_VERTICAL_SEARCH = 4;

    public record NodeScan(@Nullable Vector3i dig, @Nullable Vector3i mine, @Nonnull String diagnostics) {
    }

    @Nullable
    public static Vector3i nodeCenterColumnFor(@Nonnull Vector3d anchor, int radius, int x, int z) {
        for (Vector3i center : nodeCenterColumns(anchor, radius)) {
            if (center.x == x && center.z == z) {
                return center;
            }
        }
        for (Vector3i center : nodeCenterColumns(anchor, radius)) {
            if (Math.abs(center.x - x) + Math.abs(center.z - z) == 1) {
                return center;
            }
        }
        return null;
    }

    @Nonnull
    private static List<Vector3i> crossColumns(@Nonnull Vector3i center) {
        return Objects.requireNonNull(List.of(
                center,
                new Vector3i(center.x - 1, 0, center.z),
                new Vector3i(center.x + 1, 0, center.z),
                new Vector3i(center.x, 0, center.z - 1),
                new Vector3i(center.x, 0, center.z + 1)));
    }

    @Nonnull
    public static NodeScan scanNodes(@Nonnull World world, @Nonnull Vector3d anchor, int radius,
            boolean hasSlotOre, @Nonnull Predicate<Vector3i> failedTarget) {
        Vector3i mine = null;
        Vector3i dig = null;
        int growing = 0;
        int ready = 0;
        int blocked = 0;
        int nodeIndex = 0;
        int activeNode = -1;
        for (Vector3i center : nodeCenterColumns(anchor, radius)) {
            nodeIndex++;
            boolean centerStarted = findGrowthInColumn(world, anchor, center.x, center.z) != null
                    || findOreInColumn(world, anchor, center.x, center.z) != null;
            Vector3i nodeMine = null;
            Vector3i nodeCenterDig = null;
            Vector3i nodeArmDig = null;
            for (Vector3i column : crossColumns(center)) {
                Vector3i mineablePosition = findMineableInColumn(world, anchor, column.x, column.z);
                if (mineablePosition != null) {
                    ready++;
                    if (!failedTarget.test(mineablePosition) && (nodeMine == null
                            || mineablePosition.y > nodeMine.y)) {
                        nodeMine = mineablePosition;
                    }
                    continue;
                }
                if (findGrowthInColumn(world, anchor, column.x, column.z) != null) {
                    growing++;
                    continue;
                }
                Vector3i digPosition = findDigPositionInColumn(world, anchor, column.x, column.z);
                if (digPosition == null || failedTarget.test(digPosition)) {
                    blocked++;
                    continue;
                }
                if (column.x == center.x && column.z == center.z) {
                    if (hasSlotOre) {
                        nodeCenterDig = digPosition;
                    }
                } else if (centerStarted && nodeArmDig == null) {
                    nodeArmDig = digPosition;
                }
            }
            if (nodeMine != null && mine == null) {
                mine = nodeMine;
                activeNode = nodeIndex;
            }
            Vector3i nodeDig = nodeCenterDig != null ? nodeCenterDig : nodeArmDig;
            if (nodeDig != null && dig == null) {
                dig = nodeDig;
                if (activeNode < 0) {
                    activeNode = nodeIndex;
                }
            }
        }
        if (mine != null) {
            dig = null;
        }
        String diagnostics = "node=" + (activeNode > 0 ? activeNode : 0) + ", growing=" + growing + ", ready="
                + ready + ", noGround=" + blocked;
        return new NodeScan(dig, mine, diagnostics);
    }

    @Nullable
    public static String resolveNodeOreBlock(@Nonnull World world, @Nonnull Vector3d anchor, int radius, int x,
            int z) {
        Vector3i center = nodeCenterColumnFor(anchor, radius, x, z);
        if (center == null) {
            return null;
        }
        Vector3i growthPosition = findGrowthInColumn(world, anchor, center.x, center.z);
        if (growthPosition == null) {
            return null;
        }
        OreGrowthBlock growth = MiningGrowthUtil.getGrowth(world, growthPosition);
        return growth != null ? growth.getOreBlockId() : null;
    }

    @Nullable
    private static Vector3i findGrowthInColumn(@Nonnull World world, @Nonnull Vector3d anchor, int x, int z) {
        int anchorY = (int) Math.floor(anchor.y);
        for (int y = anchorY + NODE_VERTICAL_SEARCH; y >= anchorY - NODE_VERTICAL_SEARCH; y--) {
            Vector3i position = new Vector3i(x, y, z);
            if (MiningGrowthUtil.getGrowth(world, position) != null) {
                return position;
            }
        }
        return null;
    }

    @Nullable
    private static Vector3i findOreInColumn(@Nonnull World world, @Nonnull Vector3d anchor, int x, int z) {
        int anchorY = (int) Math.floor(anchor.y);
        for (int y = anchorY + NODE_VERTICAL_SEARCH; y >= anchorY - NODE_VERTICAL_SEARCH; y--) {
            Vector3i position = new Vector3i(x, y, z);
            if (isOreBlock(world, position)) {
                return position;
            }
        }
        return null;
    }

    @Nullable
    private static Vector3i findMineableInColumn(@Nonnull World world, @Nonnull Vector3d anchor, int x, int z) {
        int anchorY = (int) Math.floor(anchor.y);
        for (int y = anchorY + NODE_VERTICAL_SEARCH; y >= anchorY - NODE_VERTICAL_SEARCH; y--) {
            Vector3i position = new Vector3i(x, y, z);
            if (isOreBlock(world, position) || isOreReadyCandidate(world, position)) {
                return position;
            }
        }
        return null;
    }

    @Nullable
    private static Vector3i findDigPositionInColumn(@Nonnull World world, @Nonnull Vector3d anchor, int x, int z) {
        int anchorY = (int) Math.floor(anchor.y);
        for (int y = anchorY + NODE_VERTICAL_SEARCH; y >= anchorY - NODE_VERTICAL_SEARCH; y--) {
            Vector3i position = new Vector3i(x, y, z);
            if (isDigCandidate(world, position)) {
                return position;
            }
        }
        return null;
    }

    public static int nodeKindFor(@Nonnull Vector3d anchor, int radius, int x, int z) {
        for (Vector3i center : nodeCenterColumns(anchor, radius)) {
            if (center.x == x && center.z == z) {
                return OreGrowthBlock.KIND_NODE_CENTER;
            }
        }
        for (Vector3i center : nodeCenterColumns(anchor, radius)) {
            if (Math.abs(center.x - x) + Math.abs(center.z - z) == 1) {
                return OreGrowthBlock.KIND_NODE_ARM;
            }
        }
        return OreGrowthBlock.KIND_RANDOM;
    }

    public static boolean isOreBlock(@Nonnull World world, @Nonnull Vector3i position) {
        String blockId = FieldCandidates
                .getBlockId(FieldCandidates.getBlockType(world, position.x, position.y, position.z));
        return blockId != null && WorkRecipeConfig.getInstance().isOreBlock(blockId);
    }

    @Nullable
    public static String resolveTargetOreBlock(@Nonnull ProcessingBenchBlock bench) {
        ItemContainer input = bench.getInputContainer();
        if (input == null) {
            return null;
        }
        ItemStack oreStack = input.getItemStack(WorkstationSeedUtil.SEEDBAG_SLOT);
        if (oreStack == null || oreStack.isEmpty()) {
            return null;
        }
        String itemId = oreStack.getItem().getId();
        if (itemId == null) {
            return null;
        }
        return WorkRecipeConfig.getInstance().getOreTargetBlock(itemId);
    }

}
