package com.bud.feature.work.farming;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

public class TillSoilAction extends ActionBase {

    private static final String TILLED_BLOCK_TYPE = "Soil_Dirt_Tilled";

    private static final double INTERACTION_RANGE = 1.75;

    private static boolean shouldLogDebug() {
        return true;
    }

    @Nonnull
    private final Vector3d target = new Vector3d();

    public TillSoilAction(@Nonnull BuilderActionTillSoil builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        boolean debug = shouldLogDebug();
        if (!super.canExecute(ref, role, infoProvider, dt, store) || infoProvider == null) {
            if (debug) {
                LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] TillSoilAction.canExecute: reached=true "
                        + "super/infoProvider gate rejected (infoProvider=" + infoProvider + ")");
            }
            return false;
        }
        if (!infoProvider.hasPosition()) {
            if (debug) {
                LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] TillSoilAction.canExecute: reached=true "
                        + "infoProvider has no position");
            }
            return false;
        }
        IPositionProvider positionProvider = infoProvider.getPositionProvider();
        if (positionProvider == null || !positionProvider.providePosition(target)) {
            if (debug) {
                LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] TillSoilAction.canExecute: reached=true "
                        + "no positionProvider / providePosition failed");
            }
            return false;
        }
        boolean withinInteraction = isWithinInteractionRange(ref, store);
        boolean withinField = isWithinFieldRadius(ref, store);
        if (debug) {
            Vector3d loggedTarget = new Vector3d(target);
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] TillSoilAction.canExecute: reached=true target="
                    + loggedTarget + " withinInteractionRange=" + withinInteraction + " withinFieldRadius="
                    + withinField);
        }
        return withinInteraction && withinField;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, @Nullable InfoProvider infoProvider,
            double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, infoProvider, dt, store);
        int x = (int) Math.floor(target.x);
        int y = (int) Math.floor(target.y);
        int z = (int) Math.floor(target.z);
        World world = store.getExternalData().getWorld();
        world.setBlock(x, y, z, TILLED_BLOCK_TYPE);
        LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] TillSoilAction.execute: tilled block at (" + x + ","
                + y + "," + z + ")");
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        if (bud != null) {
            bud.setWorkTarget(null);
        }
        return true;
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
