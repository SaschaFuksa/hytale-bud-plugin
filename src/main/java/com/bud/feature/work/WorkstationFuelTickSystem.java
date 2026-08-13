package com.bud.feature.work;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
import com.hypixel.hytale.builtin.adventure.farming.config.stages.BlockStateFarmingStageData;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;

public class WorkstationFuelTickSystem extends EntityTickingSystem<ChunkStore> {

    private static final short CARD_SLOT = 0;
    private static final short FEED_SLOT = 0;

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
            updateWorkTarget(workstation, boundBud, dt, store, processingBenchBlock);
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

    private static final String TILLED_SOIL_BLOCK_TYPE = "Soil_Dirt_Tilled";

    private static final String KNOWN_CROP_BASE_BLOCK_TYPE = "Plant_Crop_Carrot_Block";

    private static final int STARVATION_REPEAT_THRESHOLD = 2;

    private static void updateWorkTarget(@Nonnull WorkstationBlockEntity workstation, @Nonnull BudComponent boundBud,
            float dt, @Nonnull Store<ChunkStore> chunkStore, @Nonnull ProcessingBenchBlock processingBenchBlock) {
        Vector3d anchor = boundBud.getWorkstationAnchor();
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
            boundBud.setWorkType(null);
            boundBud.setPendingCropBlockType(null);
        }

        float cooldown = boundBud.getWorkCooldownSecondsRemaining();
        if (cooldown > 0) {
            boundBud.setWorkCooldownSecondsRemaining(Math.max(0f, cooldown - dt));
            return;
        }

        World world = chunkStore.getExternalData().getWorld();
        WorkAssignment assignment = findNextWorkAssignment(world, anchor, workstation, processingBenchBlock);
        if (assignment == null) {
            boundBud.setWorkCooldownSecondsRemaining(WorkConfig.getInstance().getIdleRetrySeconds());
            return;
        }

        if (assignment.position().equals(workstation.getLastAssignedPosition())
                && assignment.workType() == workstation.getLastAssignedWorkType()) {
            int repeats = workstation.getConsecutiveRepeatCount() + 1;
            if (repeats >= STARVATION_REPEAT_THRESHOLD) {
                // TEMPORARY DEBUG LOGGING (Phase 6 partial-field regression, Sascha) - remove
                // once
                // ingame-confirmed.
                LoggerUtil.getLogger().info(() -> "[BUD][SCAN-DEBUG] Starvation guard fired: " + assignment.workType()
                        + " at " + assignment.position() + " repeated " + repeats + "x, blacklisting.");
                workstation.addRecentlyFailedTarget(assignment.position());
                workstation.setConsecutiveRepeatCount(0);
                boundBud.setWorkCooldownSecondsRemaining(WorkConfig.getInstance().getIdleRetrySeconds());
                return;
            }
            workstation.setConsecutiveRepeatCount(repeats);
        } else {
            workstation.setConsecutiveRepeatCount(0);
        }
        workstation.setLastAssignedPosition(assignment.position());
        workstation.setLastAssignedWorkType(assignment.workType());

        boundBud.setWorkTarget(assignment.target());
        boundBud.setWorkType(assignment.workType());
        boundBud.setPendingCropBlockType(assignment.cropBlockType());
        workstation.setTargetElapsedSeconds(0f);
    }

    private record WorkAssignment(@Nonnull Vector3d target, @Nonnull Vector3i position, @Nonnull WorkType workType,
            @Nullable String cropBlockType) {
    }

    @Nullable
    private static WorkAssignment findNextWorkAssignment(@Nonnull World world, @Nonnull Vector3d anchor,
            @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock) {
        List<Vector3i> positions = serpentinePositions(anchor, WorkConfig.getInstance().getFieldRadius(),
                WorkConfig.getInstance().getFieldMaxHeight());

        Instant now = currentGameTime(world);

        int tillCount = 0;
        Vector3i tillWinner = null;
        for (Vector3i position : positions) {
            if (!workstation.isRecentlyFailedTarget(position) && isTillCandidate(world, position)) {
                tillCount++;
                if (tillWinner == null) {
                    tillWinner = position;
                }
            }
        }

        ItemStack seedStack = processingBenchBlock.getInputContainer().getItemStack(WorkstationSeedUtil.SEEDBAG_SLOT);
        String cropBlockType = WorkstationSeedUtil.resolveCropBlockType(seedStack, workstation.getWorkRole());
        int plantCount = 0;
        Vector3i plantWinner = null;
        if (cropBlockType != null) {
            for (Vector3i position : positions) {
                if (!workstation.isRecentlyFailedTarget(position) && isPlantCandidate(world, position)) {
                    plantCount++;
                    if (plantWinner == null) {
                        plantWinner = position;
                    }
                }
            }
        }

        int waterCount = 0;
        Vector3i waterWinner = null;
        for (Vector3i position : positions) {
            if (!workstation.isRecentlyFailedTarget(position) && isWaterCandidate(world, position, now)) {
                waterCount++;
                if (waterWinner == null) {
                    waterWinner = position;
                }
            }
        }

        int harvestCount = 0;
        Vector3i harvestWinner = null;
        for (Vector3i position : positions) {
            if (!workstation.isRecentlyFailedTarget(position) && isHarvestCandidate(world, position)) {
                harvestCount++;
                if (harvestWinner == null) {
                    harvestWinner = position;
                }
            }
        }

        WorkAssignment winner;
        if (tillWinner != null) {
            winner = toAssignment(tillWinner, WorkType.TILL, null);
        } else if (plantWinner != null) {
            winner = toAssignment(plantWinner, WorkType.PLANT, cropBlockType);
        } else if (waterWinner != null) {
            winner = toAssignment(waterWinner, WorkType.WATER, null);
        } else if (harvestWinner != null) {
            winner = toAssignment(harvestWinner, WorkType.HARVEST, null);
        } else {
            winner = null;
        }

        int finalTillCount = tillCount;
        int finalPlantCount = plantCount;
        int finalWaterCount = waterCount;
        int finalHarvestCount = harvestCount;
        WorkAssignment finalWinner = winner;
        int positionCount = positions.size();
        LoggerUtil.getLogger().info(() -> "[BUD][SCAN-DEBUG] anchor=" + anchor + " radius="
                + WorkConfig.getInstance().getFieldRadius() + " maxHeight="
                + WorkConfig.getInstance().getFieldMaxHeight()
                + " positionsChecked=" + positionCount + " till=" + finalTillCount + " plant=" + finalPlantCount
                + " water=" + finalWaterCount + " harvest=" + finalHarvestCount + " winner="
                + (finalWinner != null ? finalWinner.workType() + "@" + finalWinner.position() : "none"));
        if (finalWinner == null) {
            logExclusionSamples(world, workstation, positions);
        } else if (plantCount == 0 && cropBlockType != null) {
            logExclusionSamples(world, workstation, positions);
        }

        return winner;
    }

    private static void logExclusionSamples(@Nonnull World world, @Nonnull WorkstationBlockEntity workstation,
            @Nonnull List<Vector3i> positions) {
        int logged = 0;
        for (Vector3i position : positions) {
            if (logged >= 5) {
                break;
            }
            if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
                continue;
            }
            boolean recentlyFailed = workstation.isRecentlyFailedTarget(position);
            BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
            boolean isPlantable = above != null && above == BlockType.EMPTY;
            if (isPlantable && !recentlyFailed) {
                continue;
            }
            String aboveId = above != null ? above.getId() : "null";
            String aboveMaterial = above != null ? String.valueOf(above.getMaterial()) : "n/a";
            boolean isEmptySentinel = above != null && above == BlockType.EMPTY;
            logged++;
            int loggedIndex = logged;
            LoggerUtil.getLogger().info(() -> "[BUD][SCAN-DEBUG] tilled-but-excluded sample #" + loggedIndex + " at "
                    + position + " - recentlyFailed=" + recentlyFailed + " above=" + aboveId + " material="
                    + aboveMaterial + " isEmptySentinel=" + isEmptySentinel);
        }
    }

    @Nonnull
    private static WorkAssignment toAssignment(Vector3i position, @Nonnull WorkType workType,
            @Nullable String cropBlockType) {
        return new WorkAssignment(new Vector3d(position.x + 0.5, position.y + 0.5, position.z + 0.5), position,
                workType, cropBlockType);
    }

    @Nonnull
    private static Instant currentGameTime(@Nonnull World world) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        WorldTimeResource timeResource = (WorldTimeResource) entityStore
                .getResource(WorldTimeResource.getResourceType());
        return timeResource.getGameTime();
    }

    @Nullable
    private static BlockType getBlockType(@Nonnull World world, int x, int y, int z) {
        return world.getBlockType(x, y, z);
    }

    private static boolean isTilledSoil(@Nullable BlockType blockType) {
        return blockType != null && TILLED_SOIL_BLOCK_TYPE.equals(blockType.getId());
    }

    private static boolean hasFreeTopFace(@Nonnull World world, int x, int y, int z) {
        BlockType above = getBlockType(world, x, y + 1, z);
        return above != null && above.getMaterial() == BlockMaterial.Empty;
    }

    private static boolean isTillCandidate(@Nonnull World world, Vector3i position) {
        BlockType blockType = getBlockType(world, position.x, position.y, position.z);
        return blockType != null && TILLABLE_BLOCK_TYPES.contains(blockType.getId())
                && hasFreeTopFace(world, position.x, position.y, position.z);
    }

    private static boolean isPlantCandidate(@Nonnull World world, Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        return above != null && above == BlockType.EMPTY;
    }

    private static boolean isWaterCandidate(@Nonnull World world, Vector3i position, @Nonnull Instant now) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        ComponentType<ChunkStore, TilledSoilBlock> soilType = TilledSoilBlock.getComponentType();
        if (soilType == null) {
            return false;
        }
        Holder<ChunkStore> holder = world.getBlockComponentHolder(position.x, position.y, position.z);
        if (holder == null) {
            return false;
        }
        TilledSoilBlock soil = holder.getComponent(soilType);
        if (soil == null) {
            return true;
        }
        Instant wateredUntil = soil.getWateredUntil();
        return wateredUntil == null || !wateredUntil.isAfter(now);
    }

    private static boolean isHarvestCandidate(@Nonnull World world, Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        // Same Material trap as isPlantCandidate: the crop itself is Material Empty at
        // every growth
        // stage, so only a true air-identity check tells "nothing here" apart from "our
        // own crop".
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        if (above == null || above == BlockType.EMPTY) {
            return false;
        }
        BlockType cropBase = BlockType.fromString(KNOWN_CROP_BASE_BLOCK_TYPE);
        if (cropBase == null) {
            return false;
        }
        String currentState = cropBase.getStateForBlock(above);
        if (currentState == null) {
            return false;
        }
        FarmingData farming = cropBase.getFarming();
        if (farming == null) {
            return false;
        }
        Map<String, FarmingStageData[]> stages = farming.getStages();
        FarmingStageData[] stageSet = stages != null ? stages.get(farming.getStartingStageSet()) : null;
        if (stageSet == null || stageSet.length == 0) {
            return false;
        }
        FarmingStageData lastStage = stageSet[stageSet.length - 1];
        return lastStage instanceof BlockStateFarmingStageData blockStage
                && currentState.equals(blockStage.getState());
    }

    @Nonnull
    private static List<Vector3i> serpentinePositions(@Nonnull Vector3d anchor, int radius, int maxHeight) {
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
