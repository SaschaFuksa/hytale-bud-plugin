package com.bud.feature.work.farming;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
import com.bud.feature.work.AbstractWorkAction;
import com.bud.feature.work.FarmingRecipeConfig;
import com.bud.feature.work.WorkstationBlockEntity;
import com.bud.feature.work.WorkstationSeedUtil;
import com.bud.feature.work.WorkToolItems;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;

public class FarmWorkAction extends AbstractWorkAction {

    private static final String WORK_ANIMATION = "Interact";

    public FarmWorkAction(@Nonnull BuilderActionFarmWork builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    protected void executeWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store, @Nonnull World world,
            @Nonnull BudComponent bud, int x, int y, int z) {
        switch (workType) {
            case TILL -> executeTill(world, x, y, z);
            case PLANT -> executePlant(world, bud, x, y, z);
            case WATER -> executeWater(store, world, x, y, z);
            case FERTILIZE -> executeFertilize(world, x, y, z);
            case HARVEST -> executeHarvest(world, bud, x, y, z);
            default -> throw new IllegalStateException("FarmWorkAction cannot handle work type " + workType);
        }
    }

    @Nonnull
    @Override
    protected String toolItemFor(@Nonnull WorkType workType) {
        return switch (workType) {
            case TILL -> WorkToolItems.TILL_TOOL_ITEM;
            case WATER -> WorkToolItems.WATER_TOOL_ITEM;
            case PLANT -> WorkToolItems.PLANT_TOOL_ITEM;
            case FERTILIZE -> WorkToolItems.FERTILIZE_TOOL_ITEM;
            case HARVEST -> WorkToolItems.HARVEST_TOOL_ITEM;
            default -> throw new IllegalStateException("FarmWorkAction cannot handle work type " + workType);
        };
    }

    @Override
    protected float cooldownSecondsFor(@Nonnull WorkType workType) {
        WorkConfig config = WorkConfig.getInstance();
        return switch (workType) {
            case TILL -> config.getTillIntervalSeconds();
            case PLANT -> config.getPlantIntervalSeconds();
            case WATER -> config.getWaterIntervalSeconds();
            case FERTILIZE -> config.getFertilizeIntervalSeconds();
            case HARVEST -> config.getHarvestIntervalSeconds();
            default -> throw new IllegalStateException("FarmWorkAction cannot handle work type " + workType);
        };
    }

    @Nonnull
    @Override
    protected String animationNameFor(@Nonnull WorkType workType) {
        return WORK_ANIMATION;
    }

    @Override
    protected void clearPendingWorkData(@Nonnull BudComponent bud) {
        bud.setPendingCropBlockType(null);
    }

    private static void executeTill(@Nonnull World world, int x, int y, int z) {
        String tilledSoilTargetBlock = FarmingRecipeConfig.getInstance().getTilledSoilTargetBlock();
        if (tilledSoilTargetBlock == null) {
            return;
        }
        world.setBlock(x, y, z, tilledSoilTargetBlock);
        clearOvergrowth(world, x, y, z);
    }

    private static void executePlant(@Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        String cropBlockType = bud.getPendingCropBlockType();
        if (cropBlockType == null) {
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
        if (bench == null || bench.getInputContainer() == null) {
            return;
        }
        WorkstationBlockEntity workstation = anchorHolder.getComponent(WorkstationBlockEntity.getComponentType());
        if (workstation == null) {
            return;
        }
        ItemStack seedStack = bench.getInputContainer().getItemStack(WorkstationSeedUtil.SEEDBAG_SLOT);
        String liveCropBlockType = WorkstationSeedUtil.resolveCropBlockType(seedStack, workstation.getWorkRole());
        if (!cropBlockType.equals(liveCropBlockType)) {
            return;
        }
        bench.getInputContainer().removeItemStackFromSlot(WorkstationSeedUtil.SEEDBAG_SLOT, 1);
        world.setBlock(x, y + 1, z, cropBlockType);
    }

    private static void executeWater(@Nonnull Store<EntityStore> store, @Nonnull World world, int x, int y, int z) {
        Instant now = ((WorldTimeResource) store.getResource(WorldTimeResource.getResourceType())).getGameTime();
        mutateLiveTilledSoil(world, x, y, z,
                soil -> soil.setWateredUntil(now.plusSeconds(WorkConfig.getInstance().getWaterDurationSeconds())));
    }

    private static void executeFertilize(@Nonnull World world, int x, int y, int z) {
        mutateLiveTilledSoil(world, x, y, z, soil -> soil.setFertilized(true));
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

    private static void mutateLiveTilledSoil(@Nonnull World world, int x, int y, int z,
            @Nonnull java.util.function.Consumer<TilledSoilBlock> mutator) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return;
        }
        Ref<ChunkStore> ref = chunk.getBlockComponentEntity(x, y, z);
        if (ref == null) {
            ref = ensureBlockEntityOrFetchExisting(chunk, x, y, z);
        }
        if (ref == null || !ref.isValid()) {
            return;
        }
        ComponentType<ChunkStore, TilledSoilBlock> soilType = TilledSoilBlock.getComponentType();
        if (soilType == null) {
            return;
        }
        TilledSoilBlock soil = world.getChunkStore().getStore().getComponent(ref, soilType);
        if (soil == null) {
            return;
        }
        mutator.accept(soil);
        chunk.setTicking(x, y, z, true);
    }

    @Nullable
    private static Ref<ChunkStore> ensureBlockEntityOrFetchExisting(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            return BlockModule.ensureBlockEntity(chunk, x, y, z);
        } catch (IllegalArgumentException e) {
            // Block component entity already exists (stale getBlockComponentEntity lookup raced with it) -
            // fetch the existing one instead of crashing the NPC tick.
            return chunk.getBlockComponentEntity(x, y, z);
        }
    }

    private static void clearOvergrowth(@Nonnull World world, int x, int y, int z) {
        BlockType above = world.getBlockType(x, y + 1, z);
        if (above != null && above != BlockType.EMPTY) {
            world.setBlock(x, y + 1, z, BlockType.EMPTY_KEY);
        }
    }

}
