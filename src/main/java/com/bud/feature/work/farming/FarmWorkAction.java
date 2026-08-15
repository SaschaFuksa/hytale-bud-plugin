package com.bud.feature.work.farming;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkRole;
import com.bud.core.types.WorkType;
import com.bud.feature.work.FarmToolItems;
import com.bud.feature.work.FarmingRecipeConfig;
import com.bud.feature.work.WorkstationSeedUtil;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

public class FarmWorkAction extends ActionBase {

    private static final double INTERACTION_RANGE = 1.75;

    private static final String WORK_ANIMATION = "Interact";

    @Nonnull
    private final Vector3d target = new Vector3d();

    public FarmWorkAction(@Nonnull BuilderActionFarmWork builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        if (!super.canExecute(ref, role, infoProvider, dt, store) || infoProvider == null) {
            return false;
        }
        if (!infoProvider.hasPosition()) {
            return false;
        }
        IPositionProvider positionProvider = infoProvider.getPositionProvider();
        if (positionProvider == null || !positionProvider.providePosition(target)) {
            return false;
        }
        return isWithinInteractionRange(ref, store) && isWithinFieldRadius(ref, store);
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, infoProvider, dt, store);
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        if (bud == null) {
            return true;
        }
        WorkType workType = bud.getWorkType();
        if (workType == null) {
            return true;
        }
        World world = store.getExternalData().getWorld();
        int x = (int) Math.floor(target.x);
        int y = (int) Math.floor(target.y);
        int z = (int) Math.floor(target.z);

        switch (workType) {
            case TILL -> executeTill(world, x, y, z);
            case PLANT -> executePlant(world, bud, x, y, z);
            case WATER -> executeWater(store, world, x, y, z);
            case FERTILIZE -> executeFertilize(world, x, y, z);
            case HARVEST -> executeHarvest(world, bud, x, y, z);
        }
        tryEquipToolFor(ref, store, workType);
        playWorkAnimation(ref, store);

        bud.setWorkTarget(null);
        bud.setPendingCropBlockType(null);
        bud.setWorkCooldownSecondsRemaining(cooldownSecondsFor(workType));
        return true;
    }

    private static void tryEquipToolFor(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull WorkType workType) {
        try {
            equipToolFor(ref, store, workType);
        } catch (RuntimeException e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Failed to equip tool for " + workType
                    + " - work already happened, continuing without visual tool change: " + e);
        }
    }

    private static void equipToolFor(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull WorkType workType) {
        String toolItemId = switch (workType) {
            case TILL -> FarmToolItems.TILL_TOOL_ITEM;
            case WATER -> FarmToolItems.WATER_TOOL_ITEM;
            case PLANT -> FarmToolItems.PLANT_TOOL_ITEM;
            case FERTILIZE -> FarmToolItems.FERTILIZE_TOOL_ITEM;
            case HARVEST -> FarmToolItems.HARVEST_TOOL_ITEM;
        };
        InventoryHelper.useItem(ref, toolItemId, (byte) -1, store);
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
        ItemStack seedStack = bench.getInputContainer().getItemStack(WorkstationSeedUtil.SEEDBAG_SLOT);
        String liveCropBlockType = WorkstationSeedUtil.resolveCropBlockType(seedStack, WorkRole.FARMING);
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
            ref = BlockModule.ensureBlockEntity(chunk, x, y, z);
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

    private static float cooldownSecondsFor(@Nonnull WorkType workType) {
        WorkConfig config = WorkConfig.getInstance();
        return switch (workType) {
            case TILL -> config.getTillIntervalSeconds();
            case PLANT -> config.getPlantIntervalSeconds();
            case WATER -> config.getWaterIntervalSeconds();
            case FERTILIZE -> config.getFertilizeIntervalSeconds();
            case HARVEST -> config.getHarvestIntervalSeconds();
        };
    }

    private static void playWorkAnimation(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            return;
        }
        NPCEntity npc = store.getComponent(ref, npcType);
        if (npc == null) {
            return;
        }
        npc.playAnimation(ref, AnimationSlot.Status, WORK_ANIMATION, store);
    }

    private static void clearOvergrowth(@Nonnull World world, int x, int y, int z) {
        BlockType above = world.getBlockType(x, y + 1, z);
        if (above != null && above != BlockType.EMPTY) {
            world.setBlock(x, y + 1, z, BlockType.EMPTY_KEY);
        }
    }

    private boolean isWithinInteractionRange(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        if (transformType == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(ref, transformType);
        if (transform == null) {
            return false;
        }
        return transform.getPosition().distanceSquared(target) <= INTERACTION_RANGE * INTERACTION_RANGE;
    }

    private boolean isWithinFieldRadius(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        if (bud == null) {
            return false;
        }
        Vector3d anchor = bud.getWorkstationAnchor();
        if (anchor == null) {
            return false;
        }
        double dx = anchor.x - target.x;
        double dz = anchor.z - target.z;
        double horizontalDistanceSquared = dx * dx + dz * dz;
        double radius = WorkConfig.getInstance().getFieldRadius();
        double height = Math.abs(anchor.y - target.y);
        return horizontalDistanceSquared <= radius * radius && height <= WorkConfig.getInstance().getFieldMaxHeight();
    }

}
