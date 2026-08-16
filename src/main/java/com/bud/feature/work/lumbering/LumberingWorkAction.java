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
import com.bud.feature.work.WorkstationWoodUtil;
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
        return bud.getPendingFellBlockPosition();
    }

    @Nonnull
    @Override
    protected Vector3d resolveFieldRadiusCheckPosition(@Nonnull BudComponent bud) {
        Vector3i fellBlockPosition = bud.getPendingFellBlockPosition();
        if (fellBlockPosition == null) {
            return target;
        }
        // fellBlockPosition is a raw integer block coordinate; anchor/target elsewhere use the block-center
        // convention (+0.5 on x/y/z), so match that here too - otherwise this check is off by up to 1 block
        // and rejects otherwise-valid fell targets.
        return new Vector3d(fellBlockPosition.x + 0.5, fellBlockPosition.y + 0.5, fellBlockPosition.z + 0.5);
    }

    @Override
    protected void executeWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store, @Nonnull World world,
            @Nonnull BudComponent bud, int x, int y, int z) {
        if (workType != WorkType.FELL) {
            throw new IllegalStateException("LumberingWorkAction cannot handle work type " + workType);
        }
        executeFell(world, bud, x, y, z);
    }

    @Nonnull
    @Override
    protected String toolItemFor(@Nonnull WorkType workType) {
        if (workType != WorkType.FELL) {
            throw new IllegalStateException("LumberingWorkAction cannot handle work type " + workType);
        }
        return WorkToolItems.FELL_TOOL_ITEM;
    }

    @Override
    protected float cooldownSecondsFor(@Nonnull WorkType workType) {
        if (workType != WorkType.FELL) {
            throw new IllegalStateException("LumberingWorkAction cannot handle work type " + workType);
        }
        return WorkConfig.getInstance().getFellIntervalSeconds();
    }

    @Nonnull
    @Override
    protected String animationNameFor(@Nonnull WorkType workType) {
        return FELL_ANIMATION;
    }

    @Override
    protected void clearPendingWorkData(@Nonnull BudComponent bud) {
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
                .info(() -> "[BUD] executeFell at " + base + " - connected Wood_ blocks: " + connectedCount);
        if (connectedWoodBlocks.isEmpty()) {
            return;
        }
        List<ItemStack> drops = WorkstationWoodUtil.collectFellingDrops(world, connectedWoodBlocks);
        boolean hasRoom = output.canAddItemStacks(drops, false, false);
        LoggerUtil.getLogger().info(() -> "[BUD] executeFell at " + base + " - output capacity check: "
                + (hasRoom ? "OK" : "FAILED, aborting fell"));
        if (!hasRoom) {
            return;
        }
        output.addItemStacks(drops, false, false, false);
        // A trunk/root can be several blocks wide (or dip into uneven terrain), so more than one column can end
        // up with a hole after felling. Every position whose downward neighbor isn't itself part of the felled
        // tree is a genuine ground-contact point - patch each of those with dirt instead of only the one block
        // that triggered the fell.
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
        for (Vector3i position : groundContactBlocks) {
            world.setBlock(position.x, position.y, position.z, DIRT_BLOCK_ID);
        }
    }

}
