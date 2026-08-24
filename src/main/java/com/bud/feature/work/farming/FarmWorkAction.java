package com.bud.feature.work.farming;

import java.util.List;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
import com.bud.feature.work.AbstractWorkAction;
import com.bud.feature.work.WorkToolItems;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;

public class FarmWorkAction extends AbstractWorkAction {

    public FarmWorkAction(@Nonnull BuilderActionFarmWork builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    protected void executeExtraWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store,
            @Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        if (workType != WorkType.HARVEST) {
            throw new IllegalStateException("FarmWorkAction cannot handle work type " + workType);
        }
        executeHarvest(world, bud, x, y, z);
    }

    @Nonnull
    @Override
    protected String extraToolItemFor(@Nonnull WorkType workType) {
        if (workType != WorkType.HARVEST) {
            throw new IllegalStateException("FarmWorkAction cannot handle work type " + workType);
        }
        return WorkToolItems.HARVEST_TOOL_ITEM;
    }

    @Override
    protected float extraCooldownSecondsFor(@Nonnull WorkType workType) {
        if (workType != WorkType.HARVEST) {
            throw new IllegalStateException("FarmWorkAction cannot handle work type " + workType);
        }
        return WorkConfig.getInstance().getHarvestIntervalSeconds();
    }

    @Nonnull
    @Override
    protected String extraAnimationNameFor(@Nonnull WorkType workType) {
        return WORK_ANIMATION;
    }

    private static void executeHarvest(@Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        BlockType above = world.getBlockType(x, y + 1, z);
        if (above == null) {
            return;
        }
        BlockGathering gathering = above.getGathering();
        HarvestingDropType harvestType = gathering != null ? gathering.getHarvest() : null;
        if (harvestType == null) {
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
        List<ItemStack> drops = BlockHarvestUtils.getDrops(above, 1, harvestType.getItemId(),
                harvestType.getDropListId());
        if (!output.canAddItemStacks(drops, false, false)) {
            return;
        }
        output.addItemStacks(drops, false, false, false);
        world.setBlock(x, y + 1, z, BlockType.EMPTY_KEY);
    }

}
