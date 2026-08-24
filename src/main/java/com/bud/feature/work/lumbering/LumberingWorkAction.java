package com.bud.feature.work.lumbering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
import com.bud.feature.work.AbstractWorkAction;
import com.bud.feature.work.WorkToolItems;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;

public class LumberingWorkAction extends AbstractWorkAction {

    private static final String FELL_ANIMATION = "Fell";

    private static final String DIRT_BLOCK_ID = "Soil_Dirt";

    public LumberingWorkAction(@Nonnull BuilderActionLumberingWork builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Nullable
    @Override
    protected Vector3i resolveWorkBlockPosition(@Nonnull BudComponent bud) {
        Vector3i fellBlockPosition = bud.getPendingFellBlockPosition();
        return fellBlockPosition != null ? fellBlockPosition : super.resolveWorkBlockPosition(bud);
    }

    @Nonnull
    @Override
    protected Vector3d resolveFieldRadiusCheckPosition(@Nonnull BudComponent bud) {
        Vector3i fellBlockPosition = bud.getPendingFellBlockPosition();
        if (fellBlockPosition == null) {
            return target;
        }
        return new Vector3d(fellBlockPosition.x + 0.5, fellBlockPosition.y + 0.5, fellBlockPosition.z + 0.5);
    }

    @Override
    protected void executeExtraWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store,
            @Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        switch (workType) {
            case FELL -> executeFell(world, bud, x, y, z);
            case PREPARE_SOIL -> LumberingFieldScan.prepareRootSpace(world, new Vector3i(x, y, z));
            default -> throw new IllegalStateException("LumberingWorkAction cannot handle work type " + workType);
        }
    }

    @Nonnull
    @Override
    protected String extraToolItemFor(@Nonnull WorkType workType) {
        return switch (workType) {
            case FELL -> WorkToolItems.FELL_TOOL_ITEM;
            case PREPARE_SOIL -> WorkToolItems.PREPARE_SOIL_TOOL_ITEM;
            default -> throw new IllegalStateException("LumberingWorkAction cannot handle work type " + workType);
        };
    }

    @Override
    protected float extraCooldownSecondsFor(@Nonnull WorkType workType) {
        return switch (workType) {
            case FELL -> WorkConfig.getInstance().getFellIntervalSeconds();
            case PREPARE_SOIL -> WorkConfig.getInstance().getPrepareSoilIntervalSeconds();
            default -> throw new IllegalStateException("LumberingWorkAction cannot handle work type " + workType);
        };
    }

    @Nonnull
    @Override
    protected String extraAnimationNameFor(@Nonnull WorkType workType) {
        return workType == WorkType.PREPARE_SOIL ? WORK_ANIMATION : FELL_ANIMATION;
    }

    @Override
    protected void clearExtraPendingWorkData(@Nonnull BudComponent bud) {
        bud.setPendingFellBlockPosition(null);
    }

    private static void executeFell(@Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        Vector3i base = new Vector3i(x, y, z);
        if (!WorkstationWoodUtil.isWoodBlock(world.getBlockType(base.x, base.y, base.z))) {
            return;
        }
        Vector3d anchor = bud.getWorkstationAnchor();
        if (anchor == null) {
            return;
        }
        ComponentType<ChunkStore, ProcessingBenchBlock> benchType = ProcessingBenchBlock.getComponentType();
        if (benchType == null) {
            return;
        }
        int anchorX = (int) Math.floor(anchor.x);
        int anchorY = (int) Math.floor(anchor.y) - 1;
        int anchorZ = (int) Math.floor(anchor.z);
        Holder<ChunkStore> anchorHolder = world.getBlockComponentHolder(anchorX, anchorY, anchorZ);
        if (anchorHolder == null) {
            return;
        }
        ProcessingBenchBlock bench = anchorHolder.getComponent(benchType);
        if (bench == null) {
            return;
        }
        ItemContainer output = bench.getOutputContainer();
        if (output == null) {
            return;
        }
        List<Vector3i> connectedWoodBlocks = WorkstationWoodUtil.connectedWoodBlocks(world, base,
                WorkstationWoodUtil.MAX_CONNECTED_BLOCKS);
        int connectedCount = connectedWoodBlocks.size();
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] executeFell at " + base + " - connected Wood_ blocks: " + connectedCount);
        if (connectedWoodBlocks.isEmpty()) {
            return;
        }
        List<ItemStack> drops = WorkstationWoodUtil.collectFellingDrops(world, connectedWoodBlocks);
        boolean hasRoom = output.canAddItemStacks(drops, false, false);
        LoggerUtil.getLogger().fine(() -> "[BUD] executeFell at " + base + " - output capacity check: "
                + (hasRoom ? "OK" : "FAILED, aborting fell"));
        if (!hasRoom) {
            return;
        }
        output.addItemStacks(drops, false, false, false);
        Set<Vector3i> connectedWoodBlockSet = new HashSet<>(connectedWoodBlocks);
        List<Vector3i> groundContactBlocks = new ArrayList<>();
        for (Vector3i position : connectedWoodBlocks) {
            Vector3i below = new Vector3i(position.x, position.y - 1, position.z);
            if (!connectedWoodBlockSet.contains(below)) {
                groundContactBlocks.add(position);
            }
        }
        for (Vector3i position : connectedWoodBlocks) {
            world.setBlock(position.x, position.y, position.z, BlockType.EMPTY_KEY);
        }
        WorkstationWoodUtil.wakeSurroundingBlocksForPhysicsRecheck(world, connectedWoodBlocks);
        int groundLevelY = anchorY - 1;
        for (Vector3i position : groundContactBlocks) {
            int fillFromY = Math.min(position.y, groundLevelY);
            for (int fillY = fillFromY; fillY <= groundLevelY; fillY++) {
                world.setBlock(position.x, fillY, position.z, DIRT_BLOCK_ID);
            }
        }
    }

}
