package com.bud.feature.work.lumbering;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.config.WorkConfig;
import com.bud.feature.work.FieldCandidates;
import com.bud.feature.work.WorkRecipeConfig;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

public final class LumberingFieldScan {

    private static final double[] EDGE_ANGLES_DEGREES_HORIZONTAL_X = { 90, 270 };

    private static final double[] EDGE_ANGLES_DEGREES_HORIZONTAL_Z = { 0, 180 };

    private static final double[] EDGE_ANGLES_DEGREES_CROSS = { 0, 90, 180, 270 };

    private static final double[] EDGE_ANGLES_DEGREES_DIAG = { 45, 135, 225, 315 };

    private static final String SOIL_BLOCK_PREFIX = "Soil_";

    private static final String ROOT_FILL_BLOCK = "Soil_Dirt";

    public static final String GRASS_SHORT_BLOCK_ID = "Plant_Grass_Sharp";

    public static final String GRASS_TALL_BLOCK_ID = "Plant_Grass_Sharp_Tall";

    private LumberingFieldScan() {
    }

    public static boolean isDecorateCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType blockType = FieldCandidates.getBlockType(world, position.x, position.y, position.z);
        String blockId = FieldCandidates.getBlockId(blockType);
        if (blockId == null || !blockId.startsWith(SOIL_BLOCK_PREFIX)) {
            return false;
        }
        return FieldCandidates.hasFreeTopFace(world, position.x, position.y, position.z);
    }

    public static boolean isRootCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isPreparableSurface(world, position)) {
            return false;
        }
        RootSpace rootSpace = scanRootSpace(world, position);
        return !rootSpace.hasTree() && !rootSpace.blocked().isEmpty();
    }

    public static void prepareRootSpace(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isPreparableSurface(world, position)) {
            return;
        }
        RootSpace rootSpace = scanRootSpace(world, position);
        if (rootSpace.hasTree()) {
            return;
        }
        world.setBlock(position.x, position.y, position.z, ROOT_FILL_BLOCK);
        for (Vector3i blocked : rootSpace.blocked()) {
            world.setBlock(blocked.x, blocked.y, blocked.z, ROOT_FILL_BLOCK);
        }
    }

    private record RootSpace(boolean hasTree, @Nonnull List<Vector3i> blocked) {
    }

    private static boolean isPreparableSurface(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isSolidGround(world, position.x, position.y, position.z)) {
            return false;
        }
        if (!FieldCandidates.hasFreeTopFace(world, position.x, position.y, position.z)) {
            return false;
        }
        String above = FieldCandidates
                .getBlockId(FieldCandidates.getBlockType(world, position.x, position.y + 1, position.z));
        return above == null || !WorkRecipeConfig.getInstance().isSaplingBlock(above);
    }

    @Nonnull
    private static RootSpace scanRootSpace(@Nonnull World world, @Nonnull Vector3i position) {
        int radius = Math.max(0, WorkConfig.getInstance().getTreeRootRadius());
        int depth = Math.max(1, WorkConfig.getInstance().getTreeRootDepth());
        List<Vector3i> blocked = new ArrayList<>();
        for (int dy = 0; dy < depth; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = position.x + dx;
                    int y = position.y - dy;
                    int z = position.z + dz;
                    BlockType blockType = FieldCandidates.getBlockType(world, x, y, z);
                    if (WorkstationWoodUtil.isWoodBlock(blockType)) {
                        return new RootSpace(true, Objects.requireNonNull(List.of()));
                    }
                    if (!isRootPassable(blockType)) {
                        blocked.add(new Vector3i(x, y, z));
                    }
                }
            }
        }
        return new RootSpace(false, Objects.requireNonNull(blocked));
    }

    private static boolean isSolidGround(@Nonnull World world, int x, int y, int z) {
        BlockType blockType = FieldCandidates.getBlockType(world, x, y, z);
        return blockType != null && blockType.getMaterial() != BlockMaterial.Empty;
    }

    private static boolean isRootPassable(@Nullable BlockType blockType) {
        if (blockType == null || blockType.getMaterial() == BlockMaterial.Empty) {
            return true;
        }
        if (WorkstationWoodUtil.isProtectedWoodConstruction(blockType)) {
            return true;
        }
        String blockId = blockType.getId();
        if (blockId == null) {
            return false;
        }
        return blockId.startsWith(SOIL_BLOCK_PREFIX) || WorkRecipeConfig.getInstance().isTilledSoilBlock(blockId);
    }

    public static boolean isFellCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        return WorkstationWoodUtil.isWoodBlock(FieldCandidates.getBlockType(world, position.x, position.y, position.z));
    }

    @Nullable
    public static Vector3i findWalkableFellNeighbor(@Nonnull World world, @Nonnull Vector3i position) {
        Vector3i east = new Vector3i(position.x + 1, position.y, position.z);
        if (isAirBlock(world, east.x, east.y, east.z)) {
            return east;
        }
        Vector3i west = new Vector3i(position.x - 1, position.y, position.z);
        if (isAirBlock(world, west.x, west.y, west.z)) {
            return west;
        }
        Vector3i south = new Vector3i(position.x, position.y, position.z + 1);
        if (isAirBlock(world, south.x, south.y, south.z)) {
            return south;
        }
        Vector3i north = new Vector3i(position.x, position.y, position.z - 1);
        if (isAirBlock(world, north.x, north.y, north.z)) {
            return north;
        }
        return null;
    }

    private static boolean isAirBlock(@Nonnull World world, int x, int y, int z) {
        BlockType blockType = FieldCandidates.getBlockType(world, x, y, z);
        return blockType != null && blockType == BlockType.EMPTY;
    }

    @Nonnull
    public static List<Vector3i> treeEdgePositions(@Nonnull Vector3d anchor, int radius, int maxHeight,
            int edgeCount, boolean spotsOnXAxis) {
        List<Vector3i> positions = new ArrayList<>();
        if (edgeCount <= 2) {
            positions.addAll(edgePositions(anchor, radius, maxHeight, horizontalAngles(spotsOnXAxis)));
            return positions;
        }
        positions.addAll(edgePositions(anchor, radius, maxHeight, EDGE_ANGLES_DEGREES_CROSS));
        if (edgeCount > 4) {
            positions.addAll(edgePositions(anchor, radius - 1, maxHeight, EDGE_ANGLES_DEGREES_DIAG));
        }
        return positions;
    }

    /**
     * The horizontal columns (x/z only) of each tree planting spot in the field - the same spots
     * {@link #treeEdgePositions} expands vertically, used to assign a felled tree's wood blocks to their own
     * planting slot so a fell can't cross into a neighboring tree.
     */
    @Nonnull
    public static List<Vector3i> treeEdgeColumns(@Nonnull Vector3d anchor, int radius, int edgeCount,
            boolean spotsOnXAxis) {
        List<Vector3i> columns = new ArrayList<>();
        if (edgeCount <= 2) {
            columns.addAll(edgeColumns(anchor, radius, horizontalAngles(spotsOnXAxis)));
            return columns;
        }
        columns.addAll(edgeColumns(anchor, radius, EDGE_ANGLES_DEGREES_CROSS));
        if (edgeCount > 4) {
            columns.addAll(edgeColumns(anchor, radius - 1, EDGE_ANGLES_DEGREES_DIAG));
        }
        return columns;
    }

    @Nonnull
    private static double[] horizontalAngles(boolean spotsOnXAxis) {
        return Objects.requireNonNull(spotsOnXAxis ? EDGE_ANGLES_DEGREES_HORIZONTAL_X : EDGE_ANGLES_DEGREES_HORIZONTAL_Z);
    }

    @Nonnull
    private static List<Vector3i> edgePositions(@Nonnull Vector3d anchor, int radius, int maxHeight,
            double[] angles) {
        int anchorY = (int) Math.floor(anchor.y);
        List<Vector3i> positions = new ArrayList<>();
        for (Vector3i column : edgeColumns(anchor, radius, angles)) {
            for (int dy = -maxHeight; dy <= maxHeight; dy++) {
                positions.add(new Vector3i(column.x, anchorY + dy, column.z));
            }
        }
        return positions;
    }

    @Nonnull
    private static List<Vector3i> edgeColumns(@Nonnull Vector3d anchor, int radius, double[] angles) {
        int anchorX = (int) Math.floor(anchor.x);
        int anchorZ = (int) Math.floor(anchor.z);
        List<Vector3i> columns = new ArrayList<>();
        for (double angleDegrees : angles) {
            double angleRadians = Math.toRadians(angleDegrees);
            int dx = (int) Math.round(radius * Math.sin(angleRadians));
            int dz = (int) Math.round(-radius * Math.cos(angleRadians));
            columns.add(new Vector3i(anchorX + dx, 0, anchorZ + dz));
        }
        return columns;
    }

}
