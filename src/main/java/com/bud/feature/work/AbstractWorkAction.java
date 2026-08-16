package com.bud.feature.work;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.types.WorkType;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.util.InventoryHelper;

/**
 * Shared arrival-gate/dispatch skeleton for the two per-WorkRole work actions
 * ({@code com.bud.feature.work.farming.FarmWorkAction}, {@code com.bud.feature.work.lumbering.LumberingWorkAction}).
 * Holds everything that is genuinely role-agnostic (position resolution, interaction/field-radius gating, tool
 * equip/animation/cooldown plumbing); subclasses only supply their own WorkType set and block-position resolution.
 */
public abstract class AbstractWorkAction extends ActionBase {

    protected static final double INTERACTION_RANGE = 1.75;

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
            LoggerUtil.getLogger().warning(() -> "[BUD] " + logTag() + " gate 'super.canExecute' = " + superGate
                    + ", gate 'infoProvider != null' = " + infoProviderPresent);
            return false;
        }
        boolean hasPositionGate = infoProvider.hasPosition();
        if (!hasPositionGate) {
            LoggerUtil.getLogger().warning(() -> "[BUD] " + logTag() + " gate 'infoProvider.hasPosition' = false");
            return false;
        }
        IPositionProvider positionProvider = infoProvider.getPositionProvider();
        boolean positionResolved = positionProvider != null && positionProvider.providePosition(target);
        if (!positionResolved) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] " + logTag() + " gate 'positionProvider.providePosition' = false");
            return false;
        }

        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        WorkType workType = bud != null ? bud.getWorkType() : null;
        logArrivalProgressThrottled(ref, store, bud, workType);

        boolean withinInteractionRange = isWithinInteractionRange(ref, store);
        boolean withinFieldRadius = isWithinFieldRadius(ref, store);
        LoggerUtil.getLogger()
                .warning(() -> "[BUD] " + logTag() + " (workType=" + workType + ") gate 'isWithinInteractionRange' = "
                        + withinInteractionRange + ", gate 'isWithinFieldRadius' = " + withinFieldRadius);

        boolean arrived = withinInteractionRange && withinFieldRadius;
        if (arrived) {
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

    /**
     * The real block this action should act on, for both {@link #isWithinFieldRadius} and {@code execute()}'s
     * dispatch. Defaults to the resolved movement target - correct whenever the target IS the work block. Override
     * when the movement target is a walkable neighbor of the real block instead (e.g. Lumbering/FELL).
     */
    @Nullable
    protected Vector3i resolveWorkBlockPosition(@Nonnull BudComponent bud) {
        return new Vector3i((int) Math.floor(target.x), (int) Math.floor(target.y), (int) Math.floor(target.z));
    }

    /**
     * Field-radius check position, in the same "target vs. real block" sense as {@link #resolveWorkBlockPosition}.
     * Kept as a separate hook (rather than reusing {@link #resolveWorkBlockPosition}) since it needs a
     * {@link Vector3d} and must never return null - the movement target is always a safe fallback.
     */
    @Nonnull
    protected Vector3d resolveFieldRadiusCheckPosition(@Nonnull BudComponent bud) {
        return target;
    }

    /** No-op by default; override to clear a role-specific pending-assignment side channel (e.g. pending*Position). */
    protected void clearPendingWorkData(@Nonnull BudComponent bud) {
    }

    protected abstract void executeWork(@Nonnull WorkType workType, @Nonnull Store<EntityStore> store,
            @Nonnull World world, @Nonnull BudComponent bud, int x, int y, int z);

    @Nonnull
    protected abstract String toolItemFor(@Nonnull WorkType workType);

    protected abstract float cooldownSecondsFor(@Nonnull WorkType workType);

    @Nonnull
    protected abstract String animationNameFor(@Nonnull WorkType workType);

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
        LoggerUtil.getLogger().info(() -> "[BUD] " + logTag() + " arrival progress - workType=" + workType
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
        double radius = WorkConfig.getInstance().getFieldRadius();
        double height = Math.abs(anchor.y - checkPosition.y);
        return horizontalDistanceSquared <= radius * radius && height <= WorkConfig.getInstance().getFieldMaxHeight();
    }

}
