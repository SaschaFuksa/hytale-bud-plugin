package com.bud.feature.work;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkRole;
import com.bud.core.types.WorkType;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.work.reaction.LLMWorkMessageCreation;
import com.bud.feature.work.reaction.WorkEntry;
import com.bud.feature.work.reaction.WorkReactionKind;
import com.bud.llm.interaction.LLMInteractionEntry;
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
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
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

        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);

        boolean pausedByBench = !processingBenchBlock.isActive();
        setRestingSubState(boundBud, pausedByBench || workstation.isResting());

        if (pausedByBench) {
            return;
        }

        if (workstation.isResting()) {
            tryResumeFromRest(workstation, boundBud, processingBenchBlock);
            if (!workstation.isResting()) {
                updateBlockInteractionState(store, ref, processingBenchBlock, true);
            }
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
        updateBlockInteractionState(store, ref, processingBenchBlock, false);
        String restingBudId = boundBud.getBudId();
        LoggerUtil.getLogger().info(() -> "[BUD] Workstation out of fuel, Bud " + restingBudId + " is resting.");
        if (!workstation.isOutOfFuelReactionSent() && ReactionConfig.getInstance().isEnableWorkReactions()) {
            workstation.setOutOfFuelReactionSent(true);
            fireOutOfFuelReaction(workstation);
        }
    }

    private static void updateBlockInteractionState(@Nonnull Store<ChunkStore> store, @Nonnull Ref<ChunkStore> ref,
            @Nonnull ProcessingBenchBlock processingBenchBlock, boolean active) {
        Vector3i position = WorkstationBindingHandler.resolveWorkstationBlockPosition(store, ref);
        if (position == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        BlockType blockType = getBlockType(world, position.x, position.y, position.z);
        if (blockType == null) {
            return;
        }
        processingBenchBlock.setBlockInteractionState(active ? ProcessingBenchBlock.PROCESSING : "default",
                blockType, world, position.x, position.y, position.z);
    }

    private static void fireOutOfFuelReaction(@Nonnull WorkstationBlockEntity workstation) {
        BudComponent boundBud = workstation.getBoundBud();
        if (boundBud == null) {
            return;
        }
        WorkEntry workEntry = new WorkEntry(boundBud, WorkReactionKind.OUT_OF_FUEL, boundBud.getWorkType(), null);
        LLMInteractionEntry entry = new LLMInteractionEntry(LLMWorkMessageCreation.getInstance(), workEntry);
        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                OrchestratorChannel.ACTIVITY,
                workEntry,
                "workOutOfFuel",
                boundBud.getPlayerRef().getUsername(),
                entry,
                System.currentTimeMillis()));
    }

    private static void tryRebind(@Nonnull Store<ChunkStore> store, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            int index, @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            float dt) {
        ItemStack card = processingBenchBlock.getInputContainer().getItemStack(CARD_SLOT);
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
        List<Vector3i> positions = workstation.getWorkRole() == WorkRole.LUMBERING
                ? treeEdgePositions(anchor, WorkConfig.getInstance().getFieldRadius(),
                        WorkConfig.getInstance().getFieldMaxHeight(),
                        WorkConfig.getInstance().getTreeEdgePositionCount())
                : serpentinePositions(anchor, WorkConfig.getInstance().getFieldRadius(),
                        WorkConfig.getInstance().getFieldMaxHeight());

        Instant now = currentGameTime(world);

        Vector3i tillWinner = null;
        for (Vector3i position : positions) {
            if (position != null && !workstation.isRecentlyFailedTarget(position) && isTillCandidate(world, position)) {
                tillWinner = position;
                break;
            }
        }

        ItemStack seedStack = processingBenchBlock.getInputContainer().getItemStack(WorkstationSeedUtil.SEEDBAG_SLOT);
        String cropBlockType = WorkstationSeedUtil.resolveCropBlockType(seedStack, workstation.getWorkRole());
        Vector3i plantWinner = null;
        if (cropBlockType != null) {
            for (Vector3i position : positions) {
                if (position != null && !workstation.isRecentlyFailedTarget(position)
                        && isPlantCandidate(world, position)
                        && !isTooCloseToExistingTree(world, workstation.getWorkRole(), position)) {
                    plantWinner = position;
                    break;
                }
            }
        }

        // bonus matters.
        Vector3i waterNewWinner = null;
        for (Vector3i position : positions) {
            if (position != null && !workstation.isRecentlyFailedTarget(position)
                    && isNeverWateredCandidate(world, position)) {
                waterNewWinner = position;
                break;
            }
        }

        Vector3i fertilizeWinner = null;
        for (Vector3i position : positions) {
            if (position != null && !workstation.isRecentlyFailedTarget(position)
                    && isFertilizeCandidate(world, position)) {
                fertilizeWinner = position;
                break;
            }
        }

        Vector3i harvestWinner = null;
        for (Vector3i position : positions) {
            if (position == null) {
                continue;
            }
            if (workstation.isRecentlyFailedTarget(position) || workstation.isHarvestOutputLocked(position)) {
                continue;
            }
            if (!isHarvestCandidate(world, position)) {
                continue;
            }
            if (!hasHarvestOutputRoom(world, position, processingBenchBlock)) {
                workstation.lockHarvestOutputTarget(position);
                LoggerUtil.getLogger().warning(() -> "[BUD] Workstation output has no room for the ripe tile at "
                        + position + " - HARVEST paused there until a slot is freed.");
                if (!workstation.isOutputFullReactionSent() && ReactionConfig.getInstance().isEnableWorkReactions()) {
                    workstation.setOutputFullReactionSent(true);
                    fireOutputFullReaction(workstation, resolveHarvestItemId(world, position));
                }
                continue;
            }
            harvestWinner = position;
            break;
        }

        Vector3i fellWinner = null;
        for (Vector3i position : positions) {
            if (position != null && !workstation.isRecentlyFailedTarget(position) && isFellCandidate(world, position)) {
                fellWinner = position;
                break;
            }
        }

        Vector3i waterRefreshWinner = null;
        if (now != null) {
            for (Vector3i position : positions) {
                if (position == null) {
                    continue;
                }
                if (!workstation.isRecentlyFailedTarget(position) && isWaterRefreshCandidate(world, position, now)) {
                    waterRefreshWinner = position;
                    break;
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
        } else if (fellWinner != null) {
            winner = toAssignment(fellWinner, WorkType.FELL, null);
        } else if (waterRefreshWinner != null) {
            winner = toAssignment(waterRefreshWinner, WorkType.WATER, null);
        } else {
            winner = null;
        }

        return winner;
    }

    @Nonnull
    private static WorkAssignment toAssignment(@Nonnull Vector3i position, @Nonnull WorkType workType,
            @Nullable String cropBlockType) {
        return new WorkAssignment(new Vector3d(position.x + 0.5, position.y + 0.5, position.z + 0.5), position,
                workType, cropBlockType);
    }

    @Nullable
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

    private static String getBlockId(@Nullable BlockType blockType) {
        if (blockType == null) {
            return null;
        }
        return blockType.getId();
    }

    private static boolean isTilledSoil(@Nullable BlockType blockType) {
        String blockId = getBlockId(blockType);
        if (blockId == null) {
            return false;
        }
        return FarmingRecipeConfig.getInstance().isTilledSoilBlock(blockId);
    }

    private static boolean hasFreeTopFace(@Nonnull World world, int x, int y, int z) {
        BlockType above = getBlockType(world, x, y + 1, z);
        return above != null && above.getMaterial() == BlockMaterial.Empty;
    }

    private static boolean isTillCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType blockType = getBlockType(world, position.x, position.y, position.z);
        if (blockType == null) {
            return false;
        }
        String blockId = getBlockId(blockType);
        if (blockId == null) {
            return false;
        }
        return FarmingRecipeConfig.getInstance().isTillableBlock(blockId)
                && hasFreeTopFace(world, position.x, position.y, position.z);
    }

    private static boolean isPlantCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        if (!isTilledSoil(getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        return above != null && above == BlockType.EMPTY;
    }

    private static boolean isTooCloseToExistingTree(@Nonnull World world, @Nonnull WorkRole workRole,
            @Nonnull Vector3i position) {
        if (workRole != WorkRole.LUMBERING) {
            return false;
        }
        FarmingRecipeConfig.SeedTargetPattern pattern = FarmingRecipeConfig.getInstance()
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

    private static boolean isNeverWateredCandidate(@Nonnull World world, @Nonnull Vector3i position) {
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

    private static boolean isWaterRefreshCandidate(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull Instant now) {
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

    private static boolean isFertilizeCandidate(@Nonnull World world, @Nonnull Vector3i position) {
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

    private static boolean isHarvestCandidate(@Nonnull World world, @Nonnull Vector3i position) {
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

    private static boolean isFellCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        return WorkstationWoodUtil.isWoodBlock(getBlockType(world, position.x, position.y + 1, position.z));
    }

    private static boolean hasHarvestOutputRoom(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull ProcessingBenchBlock processingBenchBlock) {
        ItemContainer output = processingBenchBlock.getOutputContainer();
        if (output == null) {
            return false;
        }
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        BlockGathering gathering = above != null ? above.getGathering() : null;
        HarvestingDropType harvest = gathering != null ? gathering.getHarvest() : null;
        String itemId = harvest != null ? harvest.getItemId() : null;
        if (itemId == null) {
            return true;
        }
        return output.canAddItemStacks(List.of(new ItemStack(itemId, 1)), false, false);
    }

    @Nullable
    private static String resolveHarvestItemId(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType above = getBlockType(world, position.x, position.y + 1, position.z);
        BlockGathering gathering = above != null ? above.getGathering() : null;
        HarvestingDropType harvest = gathering != null ? gathering.getHarvest() : null;
        return harvest != null ? harvest.getItemId() : null;
    }

    private static void fireOutputFullReaction(@Nonnull WorkstationBlockEntity workstation,
            @Nullable String blockedItemId) {
        BudComponent boundBud = workstation.getBoundBud();
        if (boundBud == null) {
            return;
        }
        WorkEntry workEntry = new WorkEntry(boundBud, WorkReactionKind.OUTPUT_FULL, boundBud.getWorkType(),
                blockedItemId);
        LLMInteractionEntry entry = new LLMInteractionEntry(LLMWorkMessageCreation.getInstance(), workEntry);
        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                OrchestratorChannel.ACTIVITY,
                workEntry,
                "workOutputFull",
                boundBud.getPlayerRef().getUsername(),
                entry,
                System.currentTimeMillis()));
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

    private static final double[] EDGE_ANGLES_DEGREES_CROSS = { 0, 90, 180, 270 };
    private static final double[] EDGE_ANGLES_DEGREES_DIAG = { 45, 135, 225, 315 };

    @Nonnull
    private static List<Vector3i> treeEdgePositions(@Nonnull Vector3d anchor, int radius, int maxHeight,
            int edgeCount) {
        List<Vector3i> positions = new ArrayList<>();
        positions.addAll(calc_edge_positions(anchor, radius, maxHeight, EDGE_ANGLES_DEGREES_CROSS));
        if (edgeCount > 4) {
            positions.addAll(calc_edge_positions(anchor, radius - 1, maxHeight, EDGE_ANGLES_DEGREES_DIAG));
        }
        return positions;
    }

    private static List<Vector3i> calc_edge_positions(@Nonnull Vector3d anchor, int radius, int maxHeight,
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
