package com.bud.feature.work;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class FieldCandidates {

    private FieldCandidates() {
    }

    @Nullable
    public static BlockType getBlockType(@Nonnull World world, int x, int y, int z) {
        if (!isChunkLoaded(world, x, z)) {
            return null;
        }
        return world.getBlockType(x, y, z);
    }

    public static boolean isChunkLoaded(@Nonnull World world, int x, int z) {
        return world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z)) != null;
    }

    @Nullable
    public static String getBlockId(@Nullable BlockType blockType) {
        if (blockType == null) {
            return null;
        }
        return blockType.getId();
    }

    public static boolean isTilledSoil(@Nullable BlockType blockType) {
        String blockId = getBlockId(blockType);
        if (blockId == null) {
            return false;
        }
        return WorkRecipeConfig.getInstance().isTilledSoilBlock(blockId);
    }

    public static boolean hasFreeTopFace(@Nonnull World world, int x, int y, int z) {
        BlockType above = getBlockType(world, x, y + 1, z);
        return above != null && above.getMaterial() == BlockMaterial.Empty;
    }

    public static boolean isTillCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType blockType = getBlockType(world, position.x, position.y, position.z);
        String blockId = getBlockId(blockType);
        if (blockId == null) {
            return false;
        }
        return WorkRecipeConfig.getInstance().isTillableBlock(blockId)
                && hasFreeTopFace(world, position.x, position.y, position.z);
    }

    public static boolean isPlantCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        return above != null && above == BlockType.EMPTY;
    }

    public static boolean isTooCloseToExistingTree(@Nonnull WorkRole workRole, @Nonnull Vector3i position,
            @Nonnull Set<Vector3i> existingTreePositions) {
        if (workRole != WorkRole.LUMBERING) {
            return false;
        }
        int minDistance = WorkConfig.getInstance().getTreeMinDistance();
        long minDistanceSquared = (long) minDistance * minDistance;
        for (int dx = -minDistance; dx <= minDistance; dx++) {
            for (int dz = -minDistance; dz <= minDistance; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if ((long) dx * dx + (long) dz * dz > minDistanceSquared) {
                    continue;
                }
                if (existingTreePositions.contains(new Vector3i(position.x + dx, position.y + 1, position.z + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nonnull
    public static Set<Vector3i> collectExistingTreePositions(@Nonnull World world, @Nonnull Vector3d anchor,
            int fieldRadius) {
        WorkRecipeConfig.SeedTargetPattern pattern = WorkRecipeConfig.getInstance()
                .getSeedTargetPattern(WorkRole.LUMBERING);
        if (pattern == null) {
            return Objects.requireNonNull(Set.of());
        }
        int minDistance = WorkConfig.getInstance().getTreeMinDistance();
        int scanRadius = fieldRadius + minDistance;
        int anchorX = (int) Math.floor(anchor.x);
        int anchorY = (int) Math.floor(anchor.y);
        int anchorZ = (int) Math.floor(anchor.z);
        Set<Vector3i> treePositions = new HashSet<>();
        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                int x = anchorX + dx;
                int y = anchorY + 1;
                int z = anchorZ + dz;
                String blockId = getBlockId(getBlockType(world, x, y, z));
                if (blockId != null && blockId.startsWith(pattern.prefix())) {
                    treePositions.add(new Vector3i(x, y, z));
                }
            }
        }
        return treePositions;
    }

    public static boolean isNeverWateredCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        return resolveTilledSoilCandidates(world, position, null).neverWatered();
    }

    public static boolean isWaterRefreshCandidate(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull Instant now) {
        return resolveTilledSoilCandidates(world, position, now).needsWaterRefresh();
    }

    public static boolean isFertilizeCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        return resolveTilledSoilCandidates(world, position, null).needsFertilize();
    }

    public record TilledSoilCandidates(boolean neverWatered, boolean needsFertilize, boolean needsWaterRefresh) {

        @Nonnull
        public static final TilledSoilCandidates NONE = new TilledSoilCandidates(false, false, false);
    }

    @Nonnull
    public static TilledSoilCandidates resolveTilledSoilCandidates(@Nonnull World world, @Nonnull Vector3i position,
            @Nullable Instant now) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return TilledSoilCandidates.NONE;
        }
        TilledSoilBlock soil = getTilledSoilComponent(world, position);
        boolean neverWatered = soil == null || soil.getWateredUntil() == null;
        boolean needsFertilize = soil == null || !soil.isFertilized();
        boolean needsWaterRefresh = false;
        if (soil != null && now != null) {
            Instant wateredUntil = soil.getWateredUntil();
            needsWaterRefresh = wateredUntil != null && !wateredUntil.isAfter(now);
        }
        return new TilledSoilCandidates(neverWatered, needsFertilize, needsWaterRefresh);
    }

    @Nullable
    private static TilledSoilBlock getTilledSoilComponent(@Nonnull World world, @Nonnull Vector3i position) {
        ComponentType<ChunkStore, TilledSoilBlock> soilType = TilledSoilBlock.getComponentType();
        if (soilType == null) {
            return null;
        }
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);
        if (holder == null) {
            return null;
        }
        return holder.getComponent(soilType);
    }

    @Nonnull
    public static List<Vector3i> serpentinePositions(@Nonnull Vector3d anchor, int radius, int maxHeight) {
        int anchorX = (int) Math.floor(anchor.x);
        int anchorY = (int) Math.floor(anchor.y);
        int anchorZ = (int) Math.floor(anchor.z);
        long radiusSquared = (long) radius * radius;

        List<Vector3i> positions = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz++) {
            boolean rowForward = (dz + radius) % 2 == 0;
            int dxStart = rowForward ? -radius : radius;
            int dxEnd = rowForward ? radius : -radius;
            int dxStep = rowForward ? 1 : -1;
            for (int dx = dxStart; rowForward ? dx <= dxEnd : dx >= dxEnd; dx += dxStep) {
                long horizontalDistanceSquared = (long) dx * dx + (long) dz * dz;
                if (horizontalDistanceSquared > radiusSquared) {
                    continue;
                }
                for (int dy = -maxHeight; dy <= maxHeight; dy++) {
                    positions.add(new Vector3i(anchorX + dx, anchorY + dy, anchorZ + dz));
                }
            }
        }
        return positions;
    }

}
