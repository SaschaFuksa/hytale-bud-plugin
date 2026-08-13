package com.bud.feature.work;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.npc.role.Role;

public class WorkstationFuelTickSystem extends EntityTickingSystem<ChunkStore> {

    private static final short CARD_SLOT = 0;
    private static final short FEED_SLOT = 0;

    private static boolean shouldLogDebug() {
        return true;
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(WorkstationBlockEntity.getComponentType(),
                WorkstationCardUtil.getProcessingBenchBlockComponentType(),
                WorkstationCardUtil.getBenchBlockComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] WorkstationFuelTickSystem.tick: entered");
        WorkstationBlockEntity workstation = archetypeChunk.getComponent(index,
                WorkstationBlockEntity.getComponentType());
        if (workstation == null) {
            return;
        }
        ProcessingBenchBlock processingBenchBlock = archetypeChunk.getComponent(index,
                WorkstationCardUtil.getProcessingBenchBlockComponentType());
        if (processingBenchBlock == null || processingBenchBlock.getFuelContainer() == null
                || processingBenchBlock.getInputContainer() == null) {
            return;
        }

        BudComponent boundBud = workstation.getBoundBud();
        boolean debug = shouldLogDebug();
        if (debug) {
            boolean isActive = processingBenchBlock.isActive();
            boolean isResting = workstation.isResting();
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] tick: ticking=true boundBud="
                    + (boundBud != null) + " isResting=" + isResting + " isActive(TURN-ON)=" + isActive);
        }
        if (boundBud == null) {
            tryRebind(store, archetypeChunk, index, workstation, processingBenchBlock, dt);
            return;
        }

        boolean pausedByBench = !processingBenchBlock.isActive();
        setRestingSubState(boundBud, pausedByBench || workstation.isResting());

        if (pausedByBench) {
            return;
        }

        if (workstation.isResting()) {
            tryResumeFromRest(workstation, boundBud, processingBenchBlock);
            return;
        }

        try {
            updateWorkTarget(workstation, boundBud, dt, store.getExternalData().getWorld(), debug);
        } catch (RuntimeException e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to update work target for Workstation - "
                    + "skipping this tick: " + e);
        }

        float remaining = workstation.getFuelSecondsRemaining() - dt;
        if (remaining > 0) {
            workstation.setFuelSecondsRemaining(remaining);
            return;
        }
        if (consumeOneFuel(processingBenchBlock)) {
            workstation.setFuelSecondsRemaining(WorkConfig.getInstance().getFuelDurationSeconds());
            return;
        }
        workstation.setFuelSecondsRemaining(0f);
        workstation.setResting(true);
        String restingBudId = boundBud.getBudId();
        LoggerUtil.getLogger().info(() -> "[BUD] Workstation out of fuel, Bud " + restingBudId + " is resting.");
    }

    private static void tryRebind(@Nonnull Store<ChunkStore> store, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            int index, @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            float dt) {
        ItemStack card = processingBenchBlock.getInputContainer().getItemStack(CARD_SLOT);
        if (!WorkstationCardUtil.matchesWorkRole(card, workstation.getWorkRole())) {
            workstation.setRebindRetrySecondsRemaining(0f);
            return;
        }
        float remaining = workstation.getRebindRetrySecondsRemaining() - dt;
        if (remaining > 0) {
            workstation.setRebindRetrySecondsRemaining(remaining);
            return;
        }
        workstation.setRebindRetrySecondsRemaining(WorkConfig.getInstance().getRebindRetrySeconds());
        BenchBlock benchBlock = archetypeChunk.getComponent(index, WorkstationCardUtil.getBenchBlockComponentType());
        if (benchBlock == null) {
            return;
        }
        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        WorkstationBindingHandler.reevaluate(store, ref, workstation, processingBenchBlock, benchBlock);
    }

    private static void tryResumeFromRest(@Nonnull WorkstationBlockEntity workstation, @Nonnull BudComponent boundBud,
            @Nonnull ProcessingBenchBlock processingBenchBlock) {
        if (!consumeOneFuel(processingBenchBlock)) {
            return;
        }
        workstation.setResting(false);
        workstation.setFuelSecondsRemaining(WorkConfig.getInstance().getFuelDurationSeconds());
        String resumingBudId = boundBud.getBudId();
        LoggerUtil.getLogger().info(() -> "[BUD] Workstation refed, Bud " + resumingBudId + " resumes work.");
    }

    private static boolean consumeOneFuel(@Nonnull ProcessingBenchBlock processingBenchBlock) {
        ItemStack fuel = processingBenchBlock.getFuelContainer().getItemStack(FEED_SLOT);
        if (isEmpty(fuel)) {
            return false;
        }
        processingBenchBlock.getFuelContainer().removeItemStackFromSlot(FEED_SLOT, 1);
        return true;
    }

    private static final Set<String> TILLABLE_BLOCK_TYPES = Set.of(
            "Soil_Dirt", "Soil_Dirt_Burnt", "Soil_Dirt_Cold", "Soil_Dirt_Dry",
            "Soil_Grass", "Soil_Grass_Burnt", "Soil_Grass_Cold", "Soil_Grass_Deep",
            "Soil_Grass_Dry", "Soil_Grass_Full", "Soil_Grass_Sunny", "Soil_Leaves",
            "Soil_Mud", "Soil_Mud_Dry", "Soil_Needles", "Soil_Pathway");

    private static void updateWorkTarget(@Nonnull WorkstationBlockEntity workstation, @Nonnull BudComponent boundBud,
            float dt, @Nonnull World world, boolean debug) {
        Vector3d anchor = boundBud.getWorkstationAnchor();
        if (debug) {
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] updateWorkTarget: called anchor=" + anchor
                    + " currentTarget=" + boundBud.getWorkTarget());
        }
        if (anchor == null) {
            return;
        }
        Vector3d currentTarget = boundBud.getWorkTarget();
        if (currentTarget != null) {
            float elapsed = workstation.getTargetElapsedSeconds() + dt;
            if (elapsed < WorkConfig.getInstance().getTargetTimeoutSeconds()) {
                workstation.setTargetElapsedSeconds(elapsed);
                return;
            }
            workstation.addRecentlyFailedTarget(new Vector3i((int) Math.floor(currentTarget.x),
                    (int) Math.floor(currentTarget.y), (int) Math.floor(currentTarget.z)));
            boundBud.setWorkTarget(null);
        }

        Vector3d candidate = findNearestTillableBlock(world, anchor, workstation, debug);
        if (debug) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD-TEMP-DEBUG] updateWorkTarget: scan result candidate=" + candidate);
        }
        boundBud.setWorkTarget(candidate);
        workstation.setTargetElapsedSeconds(0f);
    }

    @Nullable
    private static BlockType getBlockType(@Nonnull World world, int x, int y, int z) {
        return world.getBlockType(x, y, z);
    }

    private static boolean isTillable(@Nullable BlockType blockType) {
        return blockType != null && TILLABLE_BLOCK_TYPES.contains(blockType.getId());
    }

    private static boolean hasFreeTopFace(@Nonnull World world, int x, int y, int z) {
        BlockType above = getBlockType(world, x, y + 1, z);
        return above != null && above.getMaterial() == BlockMaterial.Empty;
    }

    @Nullable
    private static Vector3d findNearestTillableBlock(@Nonnull World world, @Nonnull Vector3d anchor,
            @Nonnull WorkstationBlockEntity workstation, boolean debug) {
        int radius = WorkConfig.getInstance().getFieldRadius();
        int maxHeight = WorkConfig.getInstance().getFieldMaxHeight();
        int anchorX = (int) Math.floor(anchor.x);
        int anchorY = (int) Math.floor(anchor.y);
        int anchorZ = (int) Math.floor(anchor.z);
        long radiusSquared = (long) radius * radius;

        int positionsChecked = 0;
        Set<String> sampledBlockTypeNames = debug ? new LinkedHashSet<>() : null;

        Vector3i bestPosition = null;
        long bestDistanceSquared = Long.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                long horizontalDistanceSquared = (long) dx * dx + (long) dz * dz;
                if (horizontalDistanceSquared > radiusSquared) {
                    continue;
                }
                for (int dy = -maxHeight; dy <= maxHeight; dy++) {
                    Vector3i position = new Vector3i(anchorX + dx, anchorY + dy, anchorZ + dz);
                    if (workstation.isRecentlyFailedTarget(position)) {
                        continue;
                    }
                    positionsChecked++;
                    BlockType blockType = getBlockType(world, position.x, position.y, position.z);
                    if (sampledBlockTypeNames != null && sampledBlockTypeNames.size() < 15) {
                        sampledBlockTypeNames.add(blockType != null ? blockType.getId() : "null");
                    }
                    if (!isTillable(blockType)) {
                        continue;
                    }
                    if (!hasFreeTopFace(world, position.x, position.y, position.z)) {
                        continue;
                    }
                    if (horizontalDistanceSquared < bestDistanceSquared) {
                        bestDistanceSquared = horizontalDistanceSquared;
                        bestPosition = position;
                    }
                }
            }
        }
        if (debug) {
            int finalPositionsChecked = positionsChecked;
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] findNearestTillableBlock: anchor=(" + anchorX
                    + "," + anchorY + "," + anchorZ + ") radius=" + radius + " maxHeight=" + maxHeight
                    + " positionsChecked=" + finalPositionsChecked + " sampledBlockTypeNames=" + sampledBlockTypeNames);
        }
        return bestPosition != null
                ? new Vector3d(bestPosition.x + 0.5, bestPosition.y + 0.5, bestPosition.z + 0.5)
                : null;
    }

    private static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

    private static void setRestingSubState(@Nonnull BudComponent boundBud, boolean resting) {
        Role role = boundBud.getBud().getRole();
        if (role == null) {
            return;
        }
        role.getStateSupport().setSubState(resting ? "Resting" : "Default");
    }

}
