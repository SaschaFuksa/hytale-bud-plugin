package com.bud.feature.work;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class FieldCandidates {

    private FieldCandidates() {
    }

    @Nullable
    public static BlockType getBlockType(@Nonnull World world, int x, int y, int z) {
        return world.getBlockType(x, y, z);
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

    public static boolean isTooCloseToExistingTree(@Nonnull World world, @Nonnull WorkRole workRole,
            @Nonnull Vector3i position) {
        if (workRole != WorkRole.LUMBERING) {
            return false;
        }
        WorkRecipeConfig.SeedTargetPattern pattern = WorkRecipeConfig.getInstance()
                .getSeedTargetPattern(WorkRole.LUMBERING);
        if (pattern == null) {
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
                String blockId = getBlockId(getBlockType(world, position.x + dx, position.y + 1, position.z + dz));
                if (blockId != null && blockId.startsWith(pattern.prefix())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNeverWateredCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        TilledSoilBlock soil = getTilledSoilComponent(world, position);
        return soil == null || soil.getWateredUntil() == null;
    }

    public static boolean isWaterRefreshCandidate(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull Instant now) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        TilledSoilBlock soil = getTilledSoilComponent(world, position);
        if (soil == null) {
            return false;
        }
        Instant wateredUntil = soil.getWateredUntil();
        return wateredUntil != null && !wateredUntil.isAfter(now);
    }

    public static boolean isFertilizeCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        TilledSoilBlock soil = getTilledSoilComponent(world, position);
        return soil == null || !soil.isFertilized();
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
