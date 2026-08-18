package com.bud.feature.work.lumbering;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.feature.work.FieldCandidates;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;

public final class LumberingFieldScan {

    private static final double[] EDGE_ANGLES_DEGREES_HORIZONTAL = { 90, 270 };

    private static final double[] EDGE_ANGLES_DEGREES_CROSS = { 0, 90, 180, 270 };

    private static final double[] EDGE_ANGLES_DEGREES_DIAG = { 45, 135, 225, 315 };

    private LumberingFieldScan() {
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
            int edgeCount) {
        List<Vector3i> positions = new ArrayList<>();
        if (edgeCount <= 2) {
            positions.addAll(edgePositions(anchor, radius, maxHeight, EDGE_ANGLES_DEGREES_HORIZONTAL));
            return positions;
        }
        positions.addAll(edgePositions(anchor, radius, maxHeight, EDGE_ANGLES_DEGREES_CROSS));
        if (edgeCount > 4) {
            positions.addAll(edgePositions(anchor, radius - 1, maxHeight, EDGE_ANGLES_DEGREES_DIAG));
        }
        return positions;
    }

    @Nonnull
    private static List<Vector3i> edgePositions(@Nonnull Vector3d anchor, int radius, int maxHeight,
            double[] angles) {
        int anchorX = (int) Math.floor(anchor.x);
        int anchorY = (int) Math.floor(anchor.y);
        int anchorZ = (int) Math.floor(anchor.z);

        List<Vector3i> positions = new ArrayList<>();
        for (double angleDegrees : angles) {
            double angleRadians = Math.toRadians(angleDegrees);
            int dx = (int) Math.round(radius * Math.sin(angleRadians));
            int dz = (int) Math.round(-radius * Math.cos(angleRadians));
            for (int dy = -maxHeight; dy <= maxHeight; dy++) {
                positions.add(new Vector3i(anchorX + dx, anchorY + dy, anchorZ + dz));
            }
        }
        return positions;
    }

}
