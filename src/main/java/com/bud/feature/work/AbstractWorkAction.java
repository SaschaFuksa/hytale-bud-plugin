package com.bud.feature.work;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.core.config.DebugConfig;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkRole;
import com.bud.core.types.WorkType;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

public abstract class AbstractWorkAction extends ActionBase {

    protected static final double INTERACTION_RANGE = 1.75;

    protected static final String WORK_ANIMATION = "Interact";

    private static final long ARRIVAL_PROGRESS_LOG_THROTTLE_MILLIS = 1500;

    @Nonnull
    protected final Vector3d target = new Vector3d();

    private long lastArrivalProgressLogMillis;

    protected AbstractWorkAction(@Nonnull BuilderActionBase builder) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        boolean superGate = super.canExecute(ref, role, infoProvider, dt, store);
        if (!superGate || infoProvider == null) {
            boolean infoProviderPresent = infoProvider != null;
            LoggerUtil.getLogger().fine(() -> "[BUD] " + logTag() + " gate 'super.canExecute' = " + superGate
                    + ", gate 'infoProvider != null' = " + infoProviderPresent);
            return false;
        }
        boolean hasPositionGate = infoProvider.hasPosition();
        if (!hasPositionGate) {
            LoggerUtil.getLogger().fine(() -> "[BUD] " + logTag() + " gate 'infoProvider.hasPosition' = false");
            return false;
        }
        IPositionProvider positionProvider = infoProvider.getPositionProvider();
        boolean positionResolved = positionProvider != null && positionProvider.providePosition(target);
        if (!positionResolved) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] " + logTag() + " gate 'positionProvider.providePosition' = false");
            return false;
        }

        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        WorkType workType = bud != null ? bud.getWorkType() : null;
        logArrivalProgressThrottled(ref, store, bud, workType);

        boolean withinInteractionRange = isWithinInteractionRange(ref, store);
        boolean withinFieldRadius = isWithinFieldRadius(ref, store);
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] " + logTag() + " (workType=" + workType + ") gate 'isWithinInteractionRange' = "
                        + withinInteractionRange + ", gate 'isWithinFieldRadius' = " + withinFieldRadius);

        boolean arrived = withinInteractionRange && withinFieldRadius;
        if (arrived && DebugConfig.getInstance().isEnableBudDebugInfo()) {
            LoggerUtil.getLogger().info(
                    () -> "[BUD] " + logTag() + " arrived, invoking tryExecuteWork for " + workType + " at " + target);
        }
        return arrived;
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
        Vector3i workBlockPosition = resolveWorkBlockPosition(bud);
        if (workBlockPosition != null) {
            World world = store.getExternalData().getWorld();
            tryExecuteWork(workType, store, world, bud, workBlockPosition.x, workBlockPosition.y,
                    workBlockPosition.z);
        }
        tryEquipToolFor(ref, store, workType);
        playWorkAnimation(ref, store, animationNameFor(workType));

        bud.setWorkTarget(null);
        clearPendingWorkData(bud);
        bud.setWorkCooldownSecondsRemaining(cooldownSecondsFor(workType));
        return true;
    }

    @Nullable
    protected Vector3i resolveWorkBlockPosition(@Nonnull BudComponent bud) {
        return new Vector3i((int) Math.floor(target.x), (int) Math.floor(target.y), (int) Math.floor(target.z));
    }

    @Nonnull
    protected Vector3d resolveFieldRadiusCheckPosition(@Nonnull BudComponent bud) {
        return target;
    }

    protected void clearPendingWorkData(@Nonnull BudComponent bud) {
        bud.setPendingCropBlockType(null);
        clearExtraPendingWorkData(bud);
    }

    protected void clearExtraPendingWorkData(@Nonnull BudComponent bud) {
    }

    protected final void executeWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store,
            @Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z) {
        switch (workType) {
            case TILL -> executeTill(world, x, y, z);
            case PLANT -> executePlant(world, bud, x, y, z);
            case WATER -> executeWater(store, world, x, y, z);
            case FERTILIZE -> executeFertilize(world, x, y, z);
            default -> executeExtraWork(workType, store, world, bud, x, y, z);
        }
    }

    protected abstract void executeExtraWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store,
            @Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z);

    @Nonnull
    protected final String toolItemFor(@Nonnull WorkType workType) {
        return switch (workType) {
            case TILL -> WorkToolItems.TILL_TOOL_ITEM;
            case WATER -> WorkToolItems.WATER_TOOL_ITEM;
            case PLANT -> WorkToolItems.PLANT_TOOL_ITEM;
            case FERTILIZE -> WorkToolItems.FERTILIZE_TOOL_ITEM;
            default -> extraToolItemFor(workType);
        };
    }

    @Nonnull
    protected abstract String extraToolItemFor(@Nonnull WorkType workType);

    protected final float cooldownSecondsFor(@Nonnull WorkType workType) {
        WorkConfig config = WorkConfig.getInstance();
        return switch (workType) {
            case TILL -> config.getTillIntervalSeconds();
            case PLANT -> config.getPlantIntervalSeconds();
            case WATER -> config.getWaterIntervalSeconds();
            case FERTILIZE -> config.getFertilizeIntervalSeconds();
            default -> extraCooldownSecondsFor(workType);
        };
    }

    protected abstract float extraCooldownSecondsFor(@Nonnull WorkType workType);

    @Nonnull
    protected final String animationNameFor(@Nonnull WorkType workType) {
        return switch (workType) {
            case TILL, PLANT, WATER, FERTILIZE -> WORK_ANIMATION;
            default -> extraAnimationNameFor(workType);
        };
    }

    @Nonnull
    protected abstract String extraAnimationNameFor(@Nonnull WorkType workType);

    @Nonnull
    protected String logTag() {
        return Objects.requireNonNull(getClass().getSimpleName());
    }

    private void logArrivalProgressThrottled(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nullable BudComponent bud, @Nullable WorkType workType) {
        long now = System.currentTimeMillis();
        if (now - lastArrivalProgressLogMillis < ARRIVAL_PROGRESS_LOG_THROTTLE_MILLIS) {
            return;
        }
        lastArrivalProgressLogMillis = now;
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        TransformComponent transform = transformType != null ? store.getComponent(ref, transformType) : null;
        if (transform == null) {
            return;
        }
        Vector3d budPosition = transform.getPosition();
        Vector3d workTarget = bud != null ? bud.getWorkTarget() : null;
        Vector3i fellBlockPosition = bud != null ? bud.getPendingFellBlockPosition() : null;
        double horizontalDistance = Math.sqrt(square(budPosition.x - target.x) + square(budPosition.z - target.z));
        double verticalDistance = Math.abs(budPosition.y - target.y);
        LoggerUtil.getLogger().fine(() -> "[BUD] " + logTag() + " arrival progress - workType=" + workType
                + ", budPosition=" + budPosition + ", resolvedTarget=" + target + ", workTarget=" + workTarget
                + ", pendingFellBlockPosition=" + fellBlockPosition
                + ", horizontalDistance=" + horizontalDistance + ", verticalDistance=" + verticalDistance
                + ", interactionRangeThreshold=" + INTERACTION_RANGE
                + " (gate compares combined 3D distance against this threshold)");
    }

    private static double square(double value) {
        return value * value;
    }

    private void tryExecuteWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store, @Nonnull World world,
            @Nonnull BudComponent bud, int x, int y, int z) {
        try {
            executeWork(workType, store, world, bud, x, y, z);
        } catch (RuntimeException e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] " + logTag() + " failed to execute " + workType
                    + " work action - skipping this attempt (NPC tick would otherwise crash): " + e);
        }
    }

    private void tryEquipToolFor(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull WorkType workType) {
        try {
            InventoryHelper.useItem(ref, toolItemFor(workType), (byte) -1, store);
        } catch (RuntimeException e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] " + logTag() + " failed to equip tool for " + workType
                    + " - work already happened, continuing without visual tool change: " + e);
        }
    }

    private static void playWorkAnimation(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull String animationName) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            return;
        }
        NPCEntity npc = store.getComponent(ref, npcType);
        if (npc == null) {
            return;
        }
        npc.playAnimation(ref, AnimationSlot.Status, animationName, store);
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
        Vector3d checkPosition = resolveFieldRadiusCheckPosition(bud);
        double dx = anchor.x - checkPosition.x;
        double dz = anchor.z - checkPosition.z;
        double horizontalDistanceSquared = dx * dx + dz * dz;
        WorkRole workRole = BudRegistry.getInstance().get(bud.getBudId()).getWorkRole();
        double radius = WorkConfig.getInstance().getFieldRadius(workRole);
        double height = Math.abs(anchor.y - checkPosition.y);
        return horizontalDistanceSquared <= radius * radius && height <= WorkConfig.getInstance().getFieldMaxHeight();
    }

    private static void executeTill(@Nonnull World world, int x, int y, int z) {
        String tilledSoilTargetBlock = WorkRecipeConfig.getInstance().getTilledSoilTargetBlock();
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

    private static void mutateLiveTilledSoil(@Nonnull World world, int x, int y, int z,
            @Nonnull Consumer<TilledSoilBlock> mutator) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return;
        }
        Ref<ChunkStore> ref = chunk.getBlockComponentEntity(x, y, z);
        if (ref == null) {
            ref = WorldBlockEntities.ensureOrFetch(chunk, x, y, z);
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

    private static void clearOvergrowth(@Nonnull World world, int x, int y, int z) {
        BlockType above = world.getBlockType(x, y + 1, z);
        if (above != null && above != BlockType.EMPTY) {
            world.setBlock(x, y + 1, z, BlockType.EMPTY_KEY);
        }
    }

}
