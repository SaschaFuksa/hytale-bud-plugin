package com.bud.feature.work.mining;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
import com.bud.feature.work.AbstractWorkAction;
import com.bud.feature.work.BlockDrops;
import com.bud.feature.work.WorkToolItems;
import com.bud.feature.work.WorkstationSeedUtil;
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

public class MiningWorkAction extends AbstractWorkAction {

    public MiningWorkAction(@Nonnull BuilderActionMiningWork builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    protected void executeExtraWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store,
            @Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        switch (workType) {
            case DIG -> executeDig(world, bud, x, y, z);
            case MINE -> executeMine(world, bud, x, y, z);
            default -> throw new IllegalStateException("MiningWorkAction cannot handle work type " + workType);
        }
    }

    @Nonnull
    @Override
    protected String extraToolItemFor(@Nonnull WorkType workType) {
        return switch (workType) {
            case DIG -> WorkToolItems.DIG_TOOL_ITEM;
            case MINE -> WorkToolItems.MINE_TOOL_ITEM;
            default -> throw new IllegalStateException("MiningWorkAction cannot handle work type " + workType);
        };
    }

    @Override
    protected float extraCooldownSecondsFor(@Nonnull WorkType workType) {
        WorkConfig config = WorkConfig.getInstance();
        return switch (workType) {
            case DIG -> config.getDigIntervalSeconds();
            case MINE -> config.getMineIntervalSeconds();
            default -> throw new IllegalStateException("MiningWorkAction cannot handle work type " + workType);
        };
    }

    @Nonnull
    @Override
    protected String extraAnimationNameFor(@Nonnull WorkType workType) {
        return WORK_ANIMATION;
    }

    private static void executeDig(@Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);
        if (!MiningFieldScan.isDigCandidate(world, position)) {
            return;
        }
        Vector3d anchor = bud.getWorkstationAnchor();
        if (anchor == null) {
            return;
        }
        int radius = WorkConfig.getInstance().getFieldRadius();
        int nodeKind = MiningFieldScan.nodeKindFor(anchor, radius, x, z);
        if (nodeKind == OreGrowthBlock.KIND_RANDOM) {
            MiningGrowthUtil.startGrowth(world, position, nodeKind, null);
            return;
        }
        if (nodeKind == OreGrowthBlock.KIND_NODE_ARM) {
            String inherited = MiningFieldScan.resolveNodeOreBlock(world, anchor, radius, x, z);
            if (inherited == null) {
                return;
            }
            MiningGrowthUtil.startGrowth(world, position, nodeKind, inherited);
            return;
        }
        ProcessingBenchBlock bench = resolveBench(world, anchor);
        if (bench == null || bench.getInputContainer() == null) {
            return;
        }
        String targetOreBlock = MiningFieldScan.resolveTargetOreBlock(bench);
        if (targetOreBlock == null) {
            return;
        }
        bench.getInputContainer().removeItemStackFromSlot(WorkstationSeedUtil.SEEDBAG_SLOT, 1);
        MiningGrowthUtil.startGrowth(world, position, nodeKind, targetOreBlock);
        LoggerUtil.getLogger().info(() -> "[BUD] Mining node started at " + position + " - consumed one ore, "
                + "pyramid will grow into " + targetOreBlock + ".");
    }

    private static void executeMine(@Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);
        if (!MiningFieldScan.isOreReadyCandidate(world, position) && !MiningFieldScan.isOreBlock(world, position)) {
            return;
        }
        BlockType minedBlockType = world.getBlockType(x, y, z);
        Vector3d anchor = bud.getWorkstationAnchor();
        if (anchor == null) {
            return;
        }
        ProcessingBenchBlock bench = resolveBench(world, anchor);
        if (bench == null) {
            return;
        }
        ItemContainer output = bench.getOutputContainer();
        if (output == null) {
            return;
        }
        List<ItemStack> drops = BlockDrops.resolveBreakingDrops(minedBlockType);
        if (drops.isEmpty()) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Mined block at " + position
                    + " resolved no drops - nothing was added to the Workstation output.");
            return;
        }
        if (!output.canAddItemStacks(drops, false, false)) {
            return;
        }
        output.addItemStacks(drops, false, false, false);
        MiningGrowthUtil.clear(world, position);
    }

    @Nullable
    private static ProcessingBenchBlock resolveBench(@Nonnull World world, @Nonnull Vector3d anchor) {
        ComponentType<ChunkStore, ProcessingBenchBlock> benchType = ProcessingBenchBlock.getComponentType();
        if (benchType == null) {
            return null;
        }
        int anchorX = (int) Math.floor(anchor.x);
        int anchorY = (int) Math.floor(anchor.y) - 1;
        int anchorZ = (int) Math.floor(anchor.z);
        Holder<ChunkStore> anchorHolder = world.getBlockComponentHolder(anchorX, anchorY, anchorZ);
        if (anchorHolder == null) {
            return null;
        }
        return anchorHolder.getComponent(benchType);
    }

}
