package com.bud.feature.work;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.core.config.WorkConfig;
import com.bud.core.registry.BudDefinition;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.BudState;
import com.bud.core.types.WorkRole;
import com.bud.core.types.WorkType;
import com.bud.feature.LLMInteractionManager;
import com.bud.feature.bud.creation.BudSpawner;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.state.StateChangeEvent;
import com.bud.feature.work.reaction.LLMWorkMessageCreation;
import com.bud.feature.work.reaction.WorkEntry;
import com.bud.feature.work.reaction.WorkReactionKind;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.Pair;

final class WorkstationBindingHandler {

    private static final short CARD_SLOT = 0;
    private static final short FEED_SLOT = 0;
    private static final int[][] STATION_NEIGHBOUR_OFFSETS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
            { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }
    };

    private WorkstationBindingHandler() {
    }

    static void reevaluate(@Nonnull Store<ChunkStore> store, @Nonnull Ref<ChunkStore> ref,
            @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            @Nonnull BenchBlock benchBlock) {
        ItemStack card = processingBenchBlock.getInputContainer().getItemStack(CARD_SLOT);
        boolean hasValidCard = WorkstationCardUtil.matchesWorkRole(card, workstation.getWorkRole());

        if (hasValidCard) {
            assignOwner(workstation, benchBlock);
        }

        if (workstation.getBoundBud() == null) {
            if (hasValidCard) {
                bind(store, ref, workstation, processingBenchBlock, benchBlock, Objects.requireNonNull(card));
            }
        } else if (!hasValidCard) {
            despawnBoundBud(store, workstation);
        }
    }

    private static void assignOwner(@Nonnull WorkstationBlockEntity workstation, @Nonnull BenchBlock benchBlock) {
        PlayerRef viewer = resolveSoleViewer(benchBlock);
        if (viewer != null) {
            workstation.setOwnerPlayerId(viewer.getUuid());
        }
    }

    @Nullable
    private static PlayerRef resolveSoleViewer(@Nonnull BenchBlock benchBlock) {
        Map<UUID, BenchWindow> windows = benchBlock.getWindows();
        if (windows.size() != 1) {
            return null;
        }
        return windows.values().iterator().next().getPlayerRef();
    }

    private static void bind(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref,
            @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            @Nonnull BenchBlock benchBlock, @Nonnull ItemStack card) {
        UUID ownerId = workstation.getOwnerPlayerId();
        if (ownerId == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Workstation has a valid card but no owner recorded - skipping bind.");
            return;
        }
        String budId = WorkstationCardUtil.resolveBudId(card);
        if (budId == null) {
            return;
        }
        World world = chunkStore.getExternalData().getWorld();
        world.execute(() -> performBind(chunkStore, world, ref, workstation, processingBenchBlock, benchBlock,
                ownerId, budId));
    }

    private static void performBind(@Nonnull Store<ChunkStore> chunkStore, @Nonnull World world,
            @Nonnull Ref<ChunkStore> ref, @Nonnull WorkstationBlockEntity workstation,
            @Nonnull ProcessingBenchBlock processingBenchBlock, @Nonnull BenchBlock benchBlock,
            @Nonnull UUID ownerId, @Nonnull String budId) {
        if (!ref.isValid() || workstation.getBoundBud() != null) {
            return;
        }
        Store<EntityStore> entityStore = Objects.requireNonNull(world.getEntityStore().getStore());

        PlayerBudComponent ownerBuds = resolveOwnerPlayerBudComponent(entityStore, ownerId);
        if (ownerBuds == null) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] Owner of Workstation is not currently online/resolvable - skipping bind for now.");
            recordBindFailure(workstation, "owner offline");
            return;
        }
        Vector3d position = resolveSpawnPositionNextToStation(chunkStore, world, ref);
        if (position == null) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Could not resolve Workstation world position - skipping bind.");
            recordBindFailure(workstation, "no spawn position");
            return;
        }

        BudComponent existing = findOwnedBud(ownerBuds, budId);
        BudComponent bound = existing != null
                ? teleportToStation(entityStore, existing, position)
                : spawnAtStation(entityStore, ownerBuds, budId, position);
        if (bound == null) {
            final String failedBudId = budId;
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to bind Bud '" + failedBudId + "' to Workstation.");
            recordBindFailure(workstation, "spawn failed");
            return;
        }
        workstation.setBindFailureCount(0);

        bound.setCurrentState(BudState.WORKING);
        StateChangeEvent.dispatch(bound.getBud(), bound.getPlayerRef(), BudState.WORKING);
        bound.setWorkstationAnchor(resolveStationGroundPosition(chunkStore, ref));

        if (ReactionConfig.getInstance().isEnableWorkReactions()) {
            fireWorkReaction(bound, WorkReactionKind.START, bound.getWorkType(), OrchestratorChannel.ACTIVITY,
                    "workStart");
        }

        workstation.setBoundBud(bound);
        boolean hasFuel = !isEmptyStack(processingBenchBlock.getFuelContainer().getItemStack(FEED_SLOT));
        workstation.setFuelSecondsRemaining(hasFuel ? WorkConfig.getInstance().getFuelDurationSeconds() : 0f);
        workstation.setResting(!hasFuel);

        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref,
                Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()));
        if (blockStateInfo != null) {
            processingBenchBlock.setActive(true, benchBlock, blockStateInfo);
        }
    }

    private static void recordBindFailure(@Nonnull WorkstationBlockEntity workstation, @Nonnull String reason) {
        workstation.setBindFailureCount(workstation.getBindFailureCount() + 1);
        if (workstation.hasGivenUpBinding()) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Giving up binding a Bud to this Workstation after repeated "
                    + "failures (" + reason + ") - remove and reinsert the card to retry.");
        }
    }

    static void despawnBoundBud(@Nonnull Store<ChunkStore> chunkStore, @Nonnull WorkstationBlockEntity workstation) {
        BudComponent bud = workstation.getBoundBud();
        if (bud == null) {
            return;
        }
        workstation.setBoundBud(null);
        workstation.setFuelSecondsRemaining(0f);
        workstation.setResting(false);
        workstation.setTargetElapsedSeconds(0f);
        WorkType lastWorkType = bud.getWorkType();
        bud.setWorkstationAnchor(null);
        bud.setWorkTarget(null);

        World world = chunkStore.getExternalData().getWorld();
        if (ReactionConfig.getInstance().isEnableWorkReactions()) {
            fireWorkEndReactionThenDespawn(world, bud, lastWorkType);
        } else {
            world.execute(() -> performDespawn(world, bud));
        }
    }

    private static void fireWorkEndReactionThenDespawn(@Nonnull World world, @Nonnull BudComponent bud,
            @Nullable WorkType lastWorkType) {
        Thread.ofVirtual().start(() -> {
            try {
                WorkEntry workEntry = new WorkEntry(bud, WorkReactionKind.END, lastWorkType, null);
                LLMInteractionEntry entry = new LLMInteractionEntry(LLMWorkMessageCreation.getInstance(), workEntry);
                LLMInteractionManager.getInstance().processInteraction(entry);
            } catch (Exception e) {
                LoggerUtil.getLogger().warning(() -> "[BUD] Failed to fire work-end reaction: " + e.getMessage());
            } finally {
                world.execute(() -> performDespawn(world, bud));
            }
        });
    }

    private static void fireWorkReaction(@Nonnull BudComponent bud, @Nonnull WorkReactionKind kind,
            @Nullable WorkType workType, @Nonnull OrchestratorChannel channel, @Nonnull String eventType) {
        WorkEntry workEntry = new WorkEntry(bud, kind, workType, null);
        LLMInteractionEntry entry = new LLMInteractionEntry(LLMWorkMessageCreation.getInstance(), workEntry);
        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                channel,
                workEntry,
                eventType,
                bud.getPlayerRef().getUsername(),
                entry,
                System.currentTimeMillis()));
    }

    private static void performDespawn(@Nonnull World world, @Nonnull BudComponent bud) {
        Store<EntityStore> entityStore = Objects.requireNonNull(world.getEntityStore().getStore());
        NPCEntity npc = bud.getBud();
        PlayerRef ownerPlayerRef = bud.getPlayerRef();
        Ref<EntityStore> ownerEntityRef = ownerPlayerRef.getReference();
        if (ownerEntityRef != null) {
            PlayerBudComponent playerBudComponent = entityStore.getComponent(ownerEntityRef,
                    PlayerBudComponent.getComponentType());
            if (playerBudComponent != null) {
                playerBudComponent.removeCurrentBud(npc, bud.getBudId());
            }
        }
        Ref<EntityStore> budRef = npc.getReference();
        if (budRef != null && budRef.isValid()) {
            entityStore.removeEntity(budRef, RemoveReason.REMOVE);
        }
        Orchestrator.getInstance().purgeBud(ownerPlayerRef.getUsername(), bud.getBudId());
    }

    @Nullable
    private static PlayerBudComponent resolveOwnerPlayerBudComponent(@Nonnull Store<EntityStore> entityStore,
            @Nonnull UUID ownerId) {
        World world = entityStore.getExternalData().getWorld();
        for (PlayerBudComponent candidate : BudManager.getInstance().getAllPlayers(world)) {
            if (ownerId.equals(candidate.getPlayerRef().getUuid())) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private static BudComponent findOwnedBud(@Nonnull PlayerBudComponent ownerBuds, @Nonnull String budId) {
        for (NPCEntity bud : ownerBuds.getCurrentBuds()) {
            BudComponent component = BudManager.getInstance().findBudComponent(bud);
            if (component != null && component.getBudId().equals(budId)) {
                return component;
            }
        }
        return null;
    }

    @Nonnull
    private static final Map<WorkRole, String> HARVEST_TYPE_TOOL_ITEM = Objects.requireNonNull(Map.of(
            WorkRole.FARMING, WorkToolItems.HARVEST_TOOL_ITEM,
            WorkRole.LUMBERING, WorkToolItems.FELL_TOOL_ITEM));

    @Nonnull
    private static BudSpawner withWorkTools(@Nonnull BudSpawner spawner, @Nonnull BudDefinition budProfile) {
        String harvestTypeTool = HARVEST_TYPE_TOOL_ITEM.get(budProfile.getWorkRole());
        if (harvestTypeTool == null) {
            return spawner;
        }
        return spawner.addTool(WorkToolItems.TILL_TOOL_ITEM, (short) 0)
                .addTool(WorkToolItems.WATER_TOOL_ITEM, (short) 1)
                .addTool(WorkToolItems.PLANT_TOOL_ITEM, (short) 2)
                .addTool(WorkToolItems.FERTILIZE_TOOL_ITEM, (short) 3)
                .addTool(harvestTypeTool, (short) 4);
    }

    @Nullable
    private static BudComponent spawnAtStation(@Nonnull Store<EntityStore> entityStore,
            @Nonnull PlayerBudComponent ownerBuds, @Nonnull String budId, @Nonnull Vector3d position) {
        BudDefinition budProfile = BudRegistry.getInstance().get(budId);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = withWorkTools(BudSpawner
                .create(entityStore, budProfile.getNpcTypeId(), position)
                .withInventory()
                .addArmor(budProfile.getArmorId()), budProfile)
                .spawn();
        if (result == null || result.first() == null) {
            return null;
        }
        NPCEntity bud = (NPCEntity) result.second();
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null) {
            return null;
        }
        ownerBuds.addBud(bud, budId);
        BudComponent component = BudComponent.create(bud, budId, ownerBuds.getPlayerRef());
        entityStore.addComponent(ref, BudComponent.getComponentType(), component);
        return component;
    }

    @Nullable
    private static BudComponent teleportToStation(@Nonnull Store<EntityStore> entityStore,
            @Nonnull BudComponent budComponent, @Nonnull Vector3d position) {
        NPCEntity oldBud = budComponent.getBud();
        Ref<EntityStore> oldRef = oldBud.getReference();
        if (oldRef == null || !oldRef.isValid()) {
            return null;
        }
        PlayerRef playerRef = budComponent.getPlayerRef();
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            return null;
        }
        PlayerBudComponent playerBudComponent = entityStore.getComponent(playerEntityRef,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            return null;
        }

        entityStore.removeEntity(oldRef, RemoveReason.REMOVE);
        playerBudComponent.removeCurrentBud(oldBud, budComponent.getBudId());

        BudDefinition budProfile = BudRegistry.getInstance().get(budComponent.getBudId());
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = withWorkTools(BudSpawner
                .create(entityStore, budProfile.getNpcTypeId(), position)
                .withInventory()
                .addArmor(budProfile.getArmorId()), budProfile)
                .spawn();
        if (result == null || result.first() == null) {
            return null;
        }
        NPCEntity newBud = Objects.requireNonNull((NPCEntity) result.second());
        Ref<EntityStore> newRef = Objects.requireNonNull(result.first());
        budComponent.setBud(newBud);
        entityStore.addComponent(newRef, BudComponent.getComponentType(), budComponent);
        playerBudComponent.addBud(newBud, budComponent.getBudId());
        return budComponent;
    }

    @Nullable
    private static Vector3d resolveSpawnPositionNextToStation(@Nonnull Store<ChunkStore> chunkStore,
            @Nonnull World world, @Nonnull Ref<ChunkStore> ref) {
        Vector3d stationGroundPos = resolveStationGroundPosition(chunkStore, ref);
        if (stationGroundPos == null) {
            return null;
        }
        for (int[] offset : STATION_NEIGHBOUR_OFFSETS) {
            Vector3d candidate = new Vector3d(stationGroundPos.x + offset[0], stationGroundPos.y,
                    stationGroundPos.z + offset[1]);
            if (BudManager.isSpawnPositionFree(world, candidate.x, candidate.y, candidate.z)) {
                return candidate;
            }
        }
        return stationGroundPos;
    }

    @Nullable
    static Vector3i resolveWorkstationBlockPosition(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref) {
        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref,
                Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()));
        if (blockStateInfo == null) {
            return null;
        }
        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        if (!chunkRef.isValid()) {
            return null;
        }
        BlockChunk blockChunk = chunkStore.getComponent(chunkRef,
                Objects.requireNonNull(BlockChunk.getComponentType()));
        if (blockChunk == null) {
            return null;
        }
        int index = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int localY = ChunkUtil.yFromBlockInColumn(index);
        int localZ = ChunkUtil.zFromBlockInColumn(index);
        int worldX = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getX(), localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getZ(), localZ);
        return new Vector3i(worldX, localY, worldZ);
    }

    @Nullable
    static Vector3d resolveStationGroundPosition(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref) {
        Vector3i blockPosition = resolveWorkstationBlockPosition(chunkStore, ref);
        if (blockPosition == null) {
            return null;
        }
        return new Vector3d(blockPosition.x + 0.5, blockPosition.y + 1.0, blockPosition.z + 0.5);
    }

    private static boolean isEmptyStack(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

}
