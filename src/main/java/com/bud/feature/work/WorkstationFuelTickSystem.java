package com.bud.feature.work;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.config.ReactionConfig;
import com.bud.core.config.WorkConfig;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.RestPosition;
import com.bud.core.types.WorkRole;
import com.bud.core.types.WorkType;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.work.farming.FarmingFieldScan;
import com.bud.feature.work.lumbering.LumberingFieldScan;
import com.bud.feature.work.lumbering.WorkstationWoodUtil;
import com.bud.feature.work.mining.MiningFieldScan;
import com.bud.feature.work.mining.OreGrowthBlock;
import com.bud.feature.work.reaction.LLMWorkMessageCreation;
import com.bud.feature.work.reaction.WorkEntry;
import com.bud.feature.work.reaction.WorkReactionKind;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
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
        boolean restingNow = pausedByBench || workstation.isResting() || workstation.isIdleNoWork();
        setRestingSubState(boundBud, restingNow);
        refreshRestTarget(store, ref, boundBud, restingNow);

        if (pausedByBench) {
            if (!workstation.isResting() && workstation.getFuelSecondsRemaining() > 0
                    && DebugConfig.getInstance().isEnableBudDebugInfo()) {
                String pausedBudId = boundBud.getBudId();
                LoggerUtil.getLogger().info(() -> "[BUD] Workstation bench reports inactive while Bud " + pausedBudId
                        + " has fuel and is not resting - work is paused purely by ProcessingBenchBlock.isActive()"
                        + "=false. This is either the player's own 'Turn Off' (respected, no override) or the "
                        + "native Bench self-deactivating (see TBD-FINAL-BUGS.md, 'Fuel Turn on Bug') - both look "
                        + "identical from here, so we never force this back on.");
            }
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
        if (DebugConfig.getInstance().isEnableBudDebugInfo()) {
            LoggerUtil.getLogger().info(() -> "[BUD] Workstation out of fuel, Bud " + restingBudId + " is resting.");
        }
        if (!workstation.isOutOfFuelReactionSent() && ReactionConfig.getInstance().isEnableWorkReactions()) {
            workstation.setOutOfFuelReactionSent(true);
            fireOutOfFuelReaction(workstation);
        }
    }

    static void updateBlockInteractionState(@Nonnull Store<ChunkStore> store, @Nonnull Ref<ChunkStore> ref,
            @Nonnull ProcessingBenchBlock processingBenchBlock, boolean active) {
        Vector3i position = WorkstationBindingHandler.resolveWorkstationBlockPosition(store, ref);
        if (position == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        BlockType blockType = FieldCandidates.getBlockType(world, position.x, position.y, position.z);
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
        if (DebugConfig.getInstance().isEnableBudDebugInfo()) {
            LoggerUtil.getLogger().info(() -> "[BUD] Workstation refed, Bud " + resumingBudId + " resumes work.");
        }
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
            float timeout = WorkConfig.getInstance().getTargetTimeoutSeconds();
            if (elapsed < timeout) {
                workstation.setTargetElapsedSeconds(elapsed);
                if (!boundBud.isWorkTargetCorrected() && elapsed >= timeout / 2f) {
                    correctStuckWorkPosition(chunkStore, boundBud, currentTarget);
                    boundBud.setWorkTargetCorrected(true);
                }
                return;
            }
            Vector3i pendingFellBlockPosition = boundBud.getPendingFellBlockPosition();
            Vector3i failedPosition = boundBud.getWorkType() == WorkType.FELL && pendingFellBlockPosition != null
                    ? pendingFellBlockPosition
                    : new Vector3i((int) Math.floor(currentTarget.x), (int) Math.floor(currentTarget.y),
                            (int) Math.floor(currentTarget.z));
            workstation.addRecentlyFailedTarget(failedPosition);
            if (DebugConfig.getInstance().isEnableBudDebugInfo()) {
                WorkType timedOutWorkType = boundBud.getWorkType();
                String timedOutBudId = boundBud.getBudId();
                LoggerUtil.getLogger().info(() -> "[BUD] Bud " + timedOutBudId + " timed out reaching "
                        + failedPosition + " for " + timedOutWorkType + " after "
                        + WorkConfig.getInstance().getTargetTimeoutSeconds()
                        + "s - marked recently-failed, retrying with a different target.");
            }
            boundBud.setWorkTarget(null);
            boundBud.setWorkType(null);
            boundBud.setPendingCropBlockType(null);
            boundBud.setPendingFellBlockPosition(null);
        }

        float cooldown = boundBud.getWorkCooldownSecondsRemaining();
        if (cooldown > 0) {
            boundBud.setWorkCooldownSecondsRemaining(Math.max(0f, cooldown - dt));
            return;
        }

        World world = chunkStore.getExternalData().getWorld();
        WorkAssignment assignment = findNextWorkAssignment(world, anchor, workstation, processingBenchBlock);
        if (assignment == null) {
            workstation.setIdleNoWork(true);
            boundBud.setWorkCooldownSecondsRemaining(WorkConfig.getInstance().getIdleRetrySeconds());
            return;
        }

        if (assignment.position().equals(workstation.getLastAssignedPosition())
                && assignment.workType() == workstation.getLastAssignedWorkType()) {
            int repeats = workstation.getConsecutiveRepeatCount() + 1;
            if (repeats >= STARVATION_REPEAT_THRESHOLD) {
                workstation.addRecentlyFailedTarget(assignment.position());
                workstation.setConsecutiveRepeatCount(0);
                workstation.setIdleNoWork(true);
                boundBud.setWorkCooldownSecondsRemaining(WorkConfig.getInstance().getIdleRetrySeconds());
                return;
            }
            workstation.setConsecutiveRepeatCount(repeats);
        } else {
            workstation.setConsecutiveRepeatCount(0);
        }
        workstation.setLastAssignedPosition(assignment.position());
        workstation.setLastAssignedWorkType(assignment.workType());

        workstation.setIdleNoWork(false);
        boundBud.setWorkTarget(assignment.target());
        boundBud.setWorkType(assignment.workType());
        boundBud.setPendingCropBlockType(assignment.cropBlockType());
        boundBud.setPendingFellBlockPosition(assignment.workType() == WorkType.FELL ? assignment.position() : null);
        boundBud.setWorkTargetCorrected(false);
        workstation.setTargetElapsedSeconds(0f);
    }

    private record WorkAssignment(@Nonnull Vector3d target, @Nonnull Vector3i position, @Nonnull WorkType workType,
            @Nullable String cropBlockType) {
    }

    @Nullable
    private static WorkAssignment findNextWorkAssignment(@Nonnull World world, @Nonnull Vector3d anchor,
            @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock) {
        boolean isLumbering = workstation.getWorkRole() == WorkRole.LUMBERING;
        boolean isMining = workstation.getWorkRole() == WorkRole.MINING;
        WorkRole workRole = workstation.getWorkRole();
        int fieldRadius = WorkConfig.getInstance().getFieldRadius(workRole);
        int fieldMaxHeight = WorkConfig.getInstance().getFieldMaxHeight();
        Vector3i flooredAnchor = new Vector3i((int) Math.floor(anchor.x), (int) Math.floor(anchor.y),
                (int) Math.floor(anchor.z));
        List<Vector3i> positions = isLumbering
                ? workstation.cachedEdgePositions(flooredAnchor, fieldRadius, fieldMaxHeight,
                        WorkConfig.getInstance().getFieldStructureCount(workRole),
                        () -> LumberingFieldScan.treeEdgePositions(anchor, fieldRadius, fieldMaxHeight,
                                WorkConfig.getInstance().getFieldStructureCount(workRole)))
                : workstation.cachedSerpentinePositions(flooredAnchor, fieldRadius, fieldMaxHeight,
                        () -> FieldCandidates.serpentinePositions(anchor, fieldRadius, fieldMaxHeight));

        Instant now = GameClock.now(world);

        Vector3i prepareSoilWinner = null;
        Vector3i tillWinner = null;
        String cropBlockType = null;
        Vector3i plantWinner = null;
        Vector3i waterNewWinner = null;
        Vector3i fertilizeWinner = null;
        Vector3i harvestWinner = null;
        Vector3i waterRefreshWinner = null;
        if (!isMining) {
            if (isLumbering) {
                for (Vector3i position : positions) {
                    if (position != null && !workstation.isRecentlyFailedTarget(position)
                            && LumberingFieldScan.isRootCandidate(world, position)) {
                        prepareSoilWinner = position;
                        break;
                    }
                }
            }

            for (Vector3i position : positions) {
                if (position != null && !workstation.isRecentlyFailedTarget(position)
                        && FieldCandidates.isTillCandidate(world, position)) {
                    tillWinner = position;
                    break;
                }
            }

            ItemStack seedStack = processingBenchBlock.getInputContainer()
                    .getItemStack(WorkstationSeedUtil.SEEDBAG_SLOT);
            cropBlockType = WorkstationSeedUtil.resolveCropBlockType(seedStack, workstation.getWorkRole());
            if (cropBlockType != null) {
                Set<Vector3i> existingTreePositions = isLumbering
                        ? FieldCandidates.collectExistingTreePositions(world, anchor, fieldRadius)
                        : Objects.requireNonNull(Set.of());
                for (Vector3i position : positions) {
                    if (position != null && !workstation.isRecentlyFailedTarget(position)
                            && FieldCandidates.isPlantCandidate(world, position)
                            && !FieldCandidates.isTooCloseToExistingTree(workstation.getWorkRole(), position,
                                    existingTreePositions)) {
                        plantWinner = position;
                        break;
                    }
                }
            }

            for (Vector3i position : positions) {
                if (position == null || workstation.isRecentlyFailedTarget(position)) {
                    continue;
                }
                if (waterNewWinner != null && fertilizeWinner != null && (now == null || waterRefreshWinner != null)) {
                    break;
                }
                FieldCandidates.TilledSoilCandidates soilCandidates = FieldCandidates
                        .resolveTilledSoilCandidates(world, position, now);
                if (waterNewWinner == null && soilCandidates.neverWatered()) {
                    waterNewWinner = position;
                }
                if (fertilizeWinner == null && soilCandidates.needsFertilize()) {
                    fertilizeWinner = position;
                }
                if (now != null && waterRefreshWinner == null && soilCandidates.needsWaterRefresh()) {
                    waterRefreshWinner = position;
                }
            }

            if (!isLumbering) {
                for (Vector3i position : positions) {
                    if (position == null) {
                        continue;
                    }
                    if (workstation.isRecentlyFailedTarget(position) || workstation.isHarvestOutputLocked(position)) {
                        continue;
                    }
                    if (!FarmingFieldScan.isHarvestCandidate(world, position)) {
                        continue;
                    }
                    if (!FarmingFieldScan.hasHarvestOutputRoom(world, position, processingBenchBlock)) {
                        workstation.lockHarvestOutputTarget(position);
                        LoggerUtil.getLogger()
                                .warning(() -> "[BUD] Workstation output has no room for the ripe tile at "
                                        + position + " - HARVEST paused there until a slot is freed.");
                        continue;
                    }
                    harvestWinner = position;
                    break;
                }
            }
        }

        Vector3i mineWinner = null;
        Vector3i oreMineWinner = null;
        Vector3i nodeDigWinner = null;
        Vector3i digWinner = null;
        int randomGrowthCount = 0;
        String nodeDiagnostics = "";
        if (isMining) {
            int radius = fieldRadius;
            String targetOreBlock = MiningFieldScan.resolveTargetOreBlock(processingBenchBlock);
            MiningFieldScan.NodeScan nodeScan = MiningFieldScan.scanNodes(world, anchor, radius,
                    targetOreBlock != null);
            nodeDigWinner = nodeScan.dig();
            oreMineWinner = nodeScan.mine();
            nodeDiagnostics = nodeScan.diagnostics();

            List<Vector3i> randomDigCandidates = new ArrayList<>();
            for (Vector3i position : positions) {
                if (position == null) {
                    continue;
                }
                if (MiningFieldScan.nodeKindFor(anchor, radius, position.x, position.z)
                        != OreGrowthBlock.KIND_RANDOM) {
                    continue;
                }
                boolean failed = workstation.isRecentlyFailedTarget(position);
                if (MiningFieldScan.isGrowthBlock(world, position)) {
                    randomGrowthCount++;
                    if (mineWinner == null && !failed && MiningFieldScan.isOreReadyCandidate(world, position)) {
                        mineWinner = position;
                    }
                    continue;
                }
                if (!failed && MiningFieldScan.isDigCandidate(world, position)) {
                    randomDigCandidates.add(position);
                }
            }
            if (randomGrowthCount < MiningFieldScan.maxDigHoles()) {
                digWinner = pickSpacedDigCandidate(world, randomDigCandidates);
            }
        }

        Vector3i fellWinner = null;
        Vector3i fellWinnerWalkTarget = null;
        if (isLumbering) {
            List<Vector3i> fellPositions = workstation.cachedSerpentinePositions(flooredAnchor, fieldRadius,
                    fieldMaxHeight, () -> FieldCandidates.serpentinePositions(anchor, fieldRadius, fieldMaxHeight));
            List<Vector3i> rawFellCandidates = new ArrayList<>();
            for (Vector3i position : fellPositions) {
                if (position == null) {
                    continue;
                }
                if (workstation.isRecentlyFailedTarget(position) || workstation.isHarvestOutputLocked(position)) {
                    continue;
                }
                if (!LumberingFieldScan.isFellCandidate(world, position)) {
                    LoggerUtil.getLogger().fine(
                            () -> "[BUD] FELL candidate check at " + position + " - rejected: not a Wood_ block.");
                    continue;
                }
                LoggerUtil.getLogger()
                        .fine(() -> "[BUD] FELL candidate check at " + position + " - accepted (Wood_ block found).");
                rawFellCandidates.add(position);
            }

            Map<Vector3i, Vector3i> visibleFellCandidateWalkTargets = new LinkedHashMap<>();
            for (Vector3i position : rawFellCandidates) {
                @Nullable
                Vector3i walkableNeighbor = LumberingFieldScan.findWalkableFellNeighbor(world,
                        Objects.requireNonNull(position));
                if (walkableNeighbor != null) {
                    visibleFellCandidateWalkTargets.put(position, Objects.requireNonNull(walkableNeighbor));
                }
            }
            Vector3i fellTarget = null;
            for (Vector3i position : visibleFellCandidateWalkTargets.keySet()) {
                if (fellTarget == null || position.y < fellTarget.y) {
                    fellTarget = position;
                }
            }
            Vector3i fellTargetWalkTarget = fellTarget != null ? visibleFellCandidateWalkTargets.get(fellTarget) : null;

            int rawFellCandidateCount = rawFellCandidates.size();
            int visibleFellCandidateCount = visibleFellCandidateWalkTargets.size();
            Vector3i loggedFellTarget = fellTarget;
            Vector3i loggedFellTargetWalkTarget = fellTargetWalkTarget;
            LoggerUtil.getLogger().fine(() -> "[BUD] FELL scan - raw Wood_ candidates: " + rawFellCandidateCount
                    + ", visible candidates: " + visibleFellCandidateCount + ", chosen: "
                    + (loggedFellTarget != null ? loggedFellTarget.toString() : "none") + ", walk target: "
                    + (loggedFellTargetWalkTarget != null ? loggedFellTargetWalkTarget.toString() : "none"));

            if (fellTarget != null) {
                List<Vector3i> plantSpotColumns = LumberingFieldScan.treeEdgeColumns(anchor, fieldRadius,
                        WorkConfig.getInstance().getFieldStructureCount(workRole));
                WorkstationWoodUtil.WoodBlockScan scan = WorkstationWoodUtil.connectedWoodBlocks(world, fellTarget,
                        WorkstationWoodUtil.MAX_CONNECTED_BLOCKS, plantSpotColumns);
                if (scan.truncated()) {
                    Vector3i truncatedFellTarget = fellTarget;
                    LoggerUtil.getLogger().warning(() -> "[BUD] FELL at " + truncatedFellTarget + " exceeds "
                            + WorkstationWoodUtil.MAX_CONNECTED_BLOCKS
                            + " connected Wood_ blocks - aborting this tree rather than felling it partially.");
                    workstation.addRecentlyFailedTarget(fellTarget);
                }
                List<Vector3i> connectedWoodBlocks = scan.blocks();
                if (!connectedWoodBlocks.isEmpty() && !WorkstationWoodUtil.hasTrunkBlock(world, connectedWoodBlocks)) {
                    workstation.addRecentlyFailedTarget(fellTarget);
                } else if (!connectedWoodBlocks.isEmpty()
                        && !WorkstationWoodUtil.isTreeMature(world, connectedWoodBlocks)) {
                    workstation.addRecentlyFailedTarget(fellTarget);
                } else if (!connectedWoodBlocks.isEmpty()) {
                    List<ItemStack> drops = WorkstationWoodUtil.collectFellingDrops(world, connectedWoodBlocks);
                    ItemContainer output = processingBenchBlock.getOutputContainer();
                    Vector3i lockedFellTarget = fellTarget;
                    if (output == null || !output.canAddItemStacks(drops, false, false)) {
                        workstation.lockHarvestOutputTarget(fellTarget);
                        LoggerUtil.getLogger()
                                .warning(() -> "[BUD] Workstation output has no room for the felled tree at "
                                        + lockedFellTarget + " - FELL paused there until a slot is freed.");
                    } else {
                        fellWinner = fellTarget;
                        fellWinnerWalkTarget = fellTargetWalkTarget;
                    }
                }
            }
        }

        if (workstation.hasLockedHarvestOutputTargets()) {
            WorkstationOutputFullReactions.fireIfDue(workstation, null);
        }

        WorkAssignment winner;
        if (isLumbering && fellWinner != null) {
            winner = toFellAssignment(fellWinner, fellWinnerWalkTarget);
        } else if (isMining && nodeDigWinner != null) {
            winner = toAssignment(nodeDigWinner, WorkType.DIG, null);
        } else if (isMining && oreMineWinner != null) {
            winner = toAssignment(oreMineWinner, WorkType.MINE, null);
        } else if (isMining && mineWinner != null) {
            winner = toAssignment(mineWinner, WorkType.MINE, null);
        } else if (isMining && digWinner != null) {
            winner = toAssignment(digWinner, WorkType.DIG, null);
        } else if (prepareSoilWinner != null) {
            winner = toAssignment(prepareSoilWinner, WorkType.PREPARE_SOIL, null);
        } else if (tillWinner != null) {
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

        if (isMining && DebugConfig.getInstance().isEnableBudDebugInfo()) {
            WorkAssignment loggedWinner = winner;
            Vector3i loggedMineWinner = mineWinner;
            Vector3i loggedDigWinner = digWinner;
            int loggedRandomGrowthCount = randomGrowthCount;
            Vector3i loggedOreMineWinner = oreMineWinner;
            Vector3i loggedNodeDigWinner = nodeDigWinner;
            String loggedNodeDiagnostics = nodeDiagnostics;
            String loggedTargetOreBlock = MiningFieldScan.resolveTargetOreBlock(processingBenchBlock);
            LoggerUtil.getLogger().info(() -> "[BUD] Mining winner selection - random dig sites="
                    + loggedRandomGrowthCount + "/" + MiningFieldScan.maxDigHoles() + ", targetOre="
                    + (loggedTargetOreBlock != null ? loggedTargetOreBlock : "none (main nodes idle)")
                    + ", mineStone=" + loggedMineWinner + ", mineOre=" + loggedOreMineWinner
                    + ", digNode=" + loggedNodeDigWinner + " [" + loggedNodeDiagnostics + "]"
                    + ", dig=" + loggedDigWinner + " -> chosen: "
                    + (loggedWinner != null ? loggedWinner.workType() + "@" + loggedWinner.position() : "none"));
        }

        if (isLumbering) {
            WorkAssignment loggedWinner = winner;
            Vector3i loggedPrepareSoilWinner = prepareSoilWinner;
            Vector3i loggedTillWinner = tillWinner;
            Vector3i loggedPlantWinner = plantWinner;
            Vector3i loggedWaterNewWinner = waterNewWinner;
            Vector3i loggedFertilizeWinner = fertilizeWinner;
            Vector3i loggedFellWinner = fellWinner;
            Vector3i loggedWaterRefreshWinner = waterRefreshWinner;
            LoggerUtil.getLogger().fine(() -> "[BUD] Lumbering winner selection - prepareSoil=" + loggedPrepareSoilWinner
                    + ", till=" + loggedTillWinner
                    + ", plant=" + loggedPlantWinner + ", waterNew=" + loggedWaterNewWinner
                    + ", fertilize=" + loggedFertilizeWinner + ", fell=" + loggedFellWinner
                    + ", waterRefresh=" + loggedWaterRefreshWinner + " -> chosen: "
                    + (loggedWinner != null ? loggedWinner.workType() + "@" + loggedWinner.position() : "none"));
        }

        return winner;
    }

    @Nullable
    private static Vector3i pickSpacedDigCandidate(@Nonnull World world, @Nonnull List<Vector3i> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        List<Vector3i> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        for (Vector3i candidate : shuffled) {
            if (candidate != null && !MiningFieldScan.isTooCloseToGrowthBlock(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Nonnull
    private static WorkAssignment toAssignment(@Nonnull Vector3i position, @Nonnull WorkType workType,
            @Nullable String cropBlockType) {
        return new WorkAssignment(new Vector3d(position.x + 0.5, position.y + 0.5, position.z + 0.5), position,
                workType, cropBlockType);
    }

    @Nonnull
    private static WorkAssignment toFellAssignment(@Nonnull Vector3i blockPosition, @Nullable Vector3i walkTarget) {
        Vector3i movementPosition = walkTarget != null ? walkTarget : blockPosition;
        return new WorkAssignment(
                new Vector3d(movementPosition.x + 0.5, movementPosition.y + 0.5, movementPosition.z + 0.5),
                blockPosition, WorkType.FELL, null);
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

    private static final double SEAT_HORIZONTAL_RANGE = 1.4;

    private static final double SEAT_HEIGHT_TOLERANCE = 0.4;

    private static void refreshRestTarget(@Nonnull Store<ChunkStore> store, @Nonnull Ref<ChunkStore> ref,
            @Nonnull BudComponent boundBud, boolean restingNow) {
        if (!restingNow) {
            if (boundBud.getRestTarget() != null) {
                boundBud.setRestTarget(null);
            }
            boundBud.setRestSeated(false);
            return;
        }
        World world = store.getExternalData().getWorld();
        RestPosition restPosition = BudRegistry.getInstance().get(boundBud.getBudId()).getRestPosition();
        if (boundBud.getRestTarget() == null) {
            Vector3d target = switch (restPosition) {
                case ON_STATION -> WorkstationBindingHandler.resolveStationGroundPosition(store, ref);
                case NEAR_STATION -> WorkstationBindingHandler.resolveSpawnPositionNextToStation(store, world, ref);
                case NONE -> null;
            };
            boundBud.setRestTarget(target);
        }
        Vector3d restTarget = boundBud.getRestTarget();
        if (restPosition != RestPosition.ON_STATION || restTarget == null || boundBud.isRestSeated()) {
            return;
        }
        seatOnStation(world, boundBud, new Vector3d(restTarget));
    }

    private static void seatOnStation(@Nonnull World world, @Nonnull BudComponent boundBud,
            @Nonnull Vector3d target) {
        world.execute(() -> {
            Store<EntityStore> entityStore = world.getEntityStore().getStore();
            ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
            if (entityStore == null || transformType == null) {
                return;
            }
            Ref<EntityStore> budRef = boundBud.getBud().getReference();
            if (budRef == null || !budRef.isValid()) {
                return;
            }
            TransformComponent transform = entityStore.getComponent(budRef, transformType);
            if (transform == null) {
                return;
            }
            Vector3d position = transform.getPosition();
            double dx = position.x - target.x;
            double dz = position.z - target.z;
            if (dx * dx + dz * dz > SEAT_HORIZONTAL_RANGE * SEAT_HORIZONTAL_RANGE) {
                return;
            }
            if (Math.abs(position.y - target.y) > SEAT_HEIGHT_TOLERANCE) {
                transform.teleportPosition(target);
            }
            boundBud.setRestSeated(true);
        });
    }

    private static final double STUCK_CORRECT_HORIZONTAL_RANGE = 3.0;

    private static void correctStuckWorkPosition(@Nonnull Store<ChunkStore> chunkStore,
            @Nonnull BudComponent boundBud, @Nonnull Vector3d target) {
        World world = chunkStore.getExternalData().getWorld();
        world.execute(() -> {
            Store<EntityStore> entityStore = world.getEntityStore().getStore();
            ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
            if (entityStore == null || transformType == null) {
                return;
            }
            Ref<EntityStore> budRef = boundBud.getBud().getReference();
            if (budRef == null || !budRef.isValid()) {
                return;
            }
            TransformComponent transform = entityStore.getComponent(budRef, transformType);
            if (transform == null) {
                return;
            }
            Vector3d position = transform.getPosition();
            double dx = position.x - target.x;
            double dz = position.z - target.z;
            if (dx * dx + dz * dz > STUCK_CORRECT_HORIZONTAL_RANGE * STUCK_CORRECT_HORIZONTAL_RANGE) {
                return;
            }
            transform.teleportPosition(new Vector3d(target.x, target.y + 0.5, target.z));
        });
    }

}
