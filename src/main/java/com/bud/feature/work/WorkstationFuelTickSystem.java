package com.bud.feature.work;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
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
        // A different card in the slot (identity, not just role/id) than the one the last bind attempt
        // used is the "Karte neu eingelegt" state change that re-enables retries after giving up - see
        // docs/bud-worker-mode-plan.md, "Phase 6, Rebind-Retry darf nicht endlos wiederholen".
        if (card != workstation.getLastAttemptedCard()) {
            workstation.setBindFailureCount(0);
        }
        if (!WorkstationCardUtil.matchesWorkRole(card, workstation.getWorkRole())) {
            workstation.setRebindRetrySecondsRemaining(0f);
            return;
        }
        if (workstation.hasGivenUpBinding()) {
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
        workstation.setLastAttemptedCard(card);
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

        // WATER_NEW (a tilled tile that has never been watered at all) is finite per position - once
        // watered, it never returns to "never watered", it can only later need a refresh - so it belongs
        // among the other finite tiers, same reasoning as "Phase 6, Endliche vor wiederkehrender Arbeit".
        // Placed before FERTILIZE: a tile needs its first watering before a fertilizer bonus matters.
        int waterNewCount = 0;
        Vector3i waterNewWinner = null;
        for (Vector3i position : positions) {
            if (!workstation.isRecentlyFailedTarget(position) && isNeverWateredCandidate(world, position)) {
                waterNewCount++;
                if (waterNewWinner == null) {
                    waterNewWinner = position;
                }
            }
        }

        int fertilizeCount = 0;
        Vector3i fertilizeWinner = null;
        for (Vector3i position : positions) {
            if (!workstation.isRecentlyFailedTarget(position) && isFertilizeCandidate(world, position)) {
                fertilizeCount++;
                if (fertilizeWinner == null) {
                    fertilizeWinner = position;
                }
            }
        }

        // Checked once per station per scan, not per position - the output container belongs to the
        // station, not to any single tile. A full output skips the whole HARVEST tier for this scan
        // (Sascha: never assign it, don't discover "full" mid-execution) rather than leaving the Bud
        // stuck in front of a ripe tile it can't do anything useful with - same starvation-class lesson
        // as the earlier WATER-over-FERTILIZE and HARVEST-no-op fixes this phase. TILL/PLANT/FERTILIZE
        // and (if applicable) WATER_REFRESH still run normally.
        boolean outputHasRoom = hasHarvestOutputRoom(processingBenchBlock);
        if (!outputHasRoom) {
            if (!workstation.isOutputFullLogged()) {
                workstation.setOutputFullLogged(true);
                LoggerUtil.getLogger().warning(
                        () -> "[BUD] Workstation output is full - HARVEST paused until a slot is emptied.");
            }
        } else {
            workstation.setOutputFullLogged(false);
        }

        int harvestCount = 0;
        Vector3i harvestWinner = null;
        if (outputHasRoom) {
            for (Vector3i position : positions) {
                if (!workstation.isRecentlyFailedTarget(position) && isHarvestCandidate(world, position)) {
                    harvestCount++;
                    if (harvestWinner == null) {
                        harvestWinner = position;
                    }
                }
            }
        }

        // WATER_REFRESH (already watered at least once, but the duration has expired) is the one
        // genuinely recurring tier - deliberately last, below every finite tier including WATER_NEW, so
        // a field with even one still-unwatered tile finishes that before re-watering anything (Sascha:
        // otherwise the order depends on WaterDurationSeconds instead of actual field progress).
        int waterRefreshCount = 0;
        Vector3i waterRefreshWinner = null;
        for (Vector3i position : positions) {
            if (!workstation.isRecentlyFailedTarget(position) && isWaterRefreshCandidate(world, position, now)) {
                waterRefreshCount++;
                if (waterRefreshWinner == null) {
                    waterRefreshWinner = position;
                }
            }
        }

        WorkAssignment winner;
        if (tillWinner != null) {
            winner = toAssignment(tillWinner, WorkType.TILL, null);
        } else if (plantWinner != null) {
            winner = toAssignment(plantWinner, WorkType.PLANT, cropBlockType);
        } else if (waterNewWinner != null) {
            winner = toAssignment(waterNewWinner, WorkType.WATER, null);
        } else if (fertilizeWinner != null) {
            winner = toAssignment(fertilizeWinner, WorkType.FERTILIZE, null);
        } else if (harvestWinner != null) {
            winner = toAssignment(harvestWinner, WorkType.HARVEST, null);
        } else if (waterRefreshWinner != null) {
            winner = toAssignment(waterRefreshWinner, WorkType.WATER, null);
        } else {
            winner = null;
        }

        int finalTillCount = tillCount;
        int finalPlantCount = plantCount;
        int finalWaterNewCount = waterNewCount;
        int finalFertilizeCount = fertilizeCount;
        int finalHarvestCount = harvestCount;
        int finalWaterRefreshCount = waterRefreshCount;
        WorkAssignment finalWinner = winner;
        int positionCount = positions.size();
        LoggerUtil.getLogger().info(() -> "[BUD][SCAN-DEBUG] anchor=" + anchor + " radius="
                + WorkConfig.getInstance().getFieldRadius() + " maxHeight="
                + WorkConfig.getInstance().getFieldMaxHeight()
                + " positionsChecked=" + positionCount + " till=" + finalTillCount + " plant=" + finalPlantCount
                + " waterNew=" + finalWaterNewCount + " fertilize=" + finalFertilizeCount + " harvest="
                + finalHarvestCount + " waterRefresh=" + finalWaterRefreshCount
                + " winner=" + (finalWinner != null ? finalWinner.workType() + "@" + finalWinner.position() : "none"));
        if (finalWinner == null) {
            logExclusionSamples(world, workstation, positions);
            logFieldCensus(world, positions);
        } else if (plantCount == 0 && cropBlockType != null) {
            logExclusionSamples(world, workstation, positions);
            logFieldCensus(world, positions);
        }

        return winner;
    }

    private static void logFieldCensus(@Nonnull World world, @Nonnull List<Vector3i> positions) {
        java.util.Map<String, Integer> groundHistogram = new java.util.TreeMap<>();
        java.util.Map<String, Integer> aboveTilledHistogram = new java.util.TreeMap<>();
        int tilledCount = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        java.util.Set<Integer> tilledYs = new java.util.TreeSet<>();

        for (Vector3i position : positions) {
            BlockType ground = getBlockType(world, position.x, position.y, position.z);
            String groundId = ground != null ? ground.getId() : "null";
            groundHistogram.merge(groundId, 1, Integer::sum);
            if (!isTilledSoil(ground)) {
                continue;
            }
            tilledCount++;
            minX = Math.min(minX, position.x);
            maxX = Math.max(maxX, position.x);
            minZ = Math.min(minZ, position.z);
            maxZ = Math.max(maxZ, position.z);
            tilledYs.add(position.y);
            BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
            String aboveId = above != null ? above.getId() : "null";
            String sentinel = (above != null && above == BlockType.EMPTY) ? "[==EMPTY]" : "[!=EMPTY]";
            aboveTilledHistogram.merge(aboveId + sentinel, 1, Integer::sum);
        }

        int finalTilled = tilledCount;
        String bounds = finalTilled > 0
                ? "x[" + minX + ".." + maxX + "] z[" + minZ + ".." + maxZ + "] y" + tilledYs
                : "none";
        LoggerUtil.getLogger().info(() -> "[BUD][CENSUS] tilledInRange=" + finalTilled + " bounds=" + bounds);
        LoggerUtil.getLogger().info(() -> "[BUD][CENSUS] groundBlocks=" + groundHistogram);
        LoggerUtil.getLogger().info(() -> "[BUD][CENSUS] aboveTilled=" + aboveTilledHistogram);
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
        return blockType != null && blockType.getId() != null
                && FarmingRecipeConfig.getInstance().isTilledSoilBlock(blockType.getId());
    }

    private static boolean hasFreeTopFace(@Nonnull World world, int x, int y, int z) {
        BlockType above = getBlockType(world, x, y + 1, z);
        return above != null && above.getMaterial() == BlockMaterial.Empty;
    }

    private static boolean isTillCandidate(@Nonnull World world, Vector3i position) {
        BlockType blockType = getBlockType(world, position.x, position.y, position.z);
        return blockType != null && blockType.getId() != null
                && FarmingRecipeConfig.getInstance().isTillableBlock(blockType.getId())
                && hasFreeTopFace(world, position.x, position.y, position.z);
    }

    private static boolean isPlantCandidate(@Nonnull World world, Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        return above != null && above == BlockType.EMPTY;
    }

    /**
     * A tile that was never watered at all (no {@code TilledSoilBlock} component yet, or one exists but
     * {@code wateredUntil} was never set) - kept separate from {@link #isWaterRefreshCandidate} so it can
     * sit at a different priority (see the finite-vs-recurring split at the call site in
     * {@link #findNextWorkAssignment}).
     */
    private static boolean isNeverWateredCandidate(@Nonnull World world, Vector3i position) {
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
        return soil == null || soil.getWateredUntil() == null;
    }

    /**
     * A tile that was watered at least once but the duration has since expired - the genuinely
     * recurring counterpart to {@link #isNeverWateredCandidate}.
     */
    private static boolean isWaterRefreshCandidate(@Nonnull World world, Vector3i position, @Nonnull Instant now) {
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
            return false;
        }
        Instant wateredUntil = soil.getWateredUntil();
        return wateredUntil != null && !wateredUntil.isAfter(now);
    }

    /**
     * Same read-only pattern as {@link #isWaterRefreshCandidate} - a {@code Holder} copy is fine here since
     * this only reads the already-persisted {@code fertilized} flag, never mutates it (the actual write
     * in {@code FarmWorkAction} goes through the live {@code Ref}/{@code Store} path instead, same
     * reasoning as watering).
     */
    private static boolean isFertilizeCandidate(@Nonnull World world, Vector3i position) {
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
        return soil == null || !soil.isFertilized();
    }

    /**
     * Fully generic, no per-variety code or config: {@code BlockGathering.isHarvestable()}
     * (bytecode-verified: {@code harvest != null}) is the exact same signal the native harvest
     * interaction chain uses ({@code HarvestCropInteraction} -> {@code FarmingUtil.harvest0} both check
     * {@code blockType.getGathering().getHarvest()} on the placed block itself, not a resolved "base"
     * type) - a crop only declares a {@code Gathering.Harvest} entry on its final ripe growth stage
     * (verified against {@code Plant_Crop_Carrot_Block.json}: only {@code State.Definitions.StageFinal}
     * has one), so its mere presence already means "ripe". Superseded the earlier
     * {@code FarmingData}/{@code getStateForBlock} comparison hardcoded to
     * {@code Plant_Crop_Carrot_Block} - see docs/bud-worker-mode-plan.md, "Phase 7 - Erntereife
     * generisch erkannt".
     */
    private static boolean isHarvestCandidate(@Nonnull World world, Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        if (above == null || above == BlockType.EMPTY) {
            return false;
        }
        BlockGathering gathering = above.getGathering();
        return gathering != null && gathering.isHarvestable();
    }

    /**
     * A conservative capacity check, not an exact simulation of the actual drops: any completely empty
     * output slot counts as "room", even though a partially-filled but stackable slot might also still
     * fit more - see docs/bud-worker-mode-plan.md, "Phase 7 - Ernte in Output-Slots" for why this
     * deliberate simplification was chosen over resolving the drops just to check capacity.
     */
    private static boolean hasHarvestOutputRoom(@Nonnull ProcessingBenchBlock processingBenchBlock) {
        ItemContainer output = processingBenchBlock.getOutputContainer();
        if (output == null) {
            return false;
        }
        short capacity = output.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            if (isEmpty(output.getItemStack(slot))) {
                return true;
            }
        }
        return false;
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
