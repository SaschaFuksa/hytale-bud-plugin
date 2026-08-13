package com.bud.feature.work.farming;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;

/**
 * Exposes the acting Bud's {@link BudComponent#getWorkTarget()} as this Instruction's {@link InfoProvider}
 * position - matches only while a target is assigned, does no scanning or range-checking of its own (the
 * Workstation already only ever assigns a target within {@code WorkConfig.FieldRadius}). See
 * docs/bud-worker-mode-plan.md, "Phase 5 — Farming-Loop Redesign".
 */
public class WorkTargetSensor extends SensorBase {

    @Nonnull
    private final PositionProvider positionProvider = new PositionProvider();

    public WorkTargetSensor(@Nonnull BuilderWorkTargetSensor builder) {
        super(builder);
    }

    @Override
    public boolean matches(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, double dt,
            @Nonnull Store<EntityStore> store) {
        if (!super.matches(ref, role, dt, store)) {
            positionProvider.clear();
            return false;
        }
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        Vector3d target = bud != null ? bud.getWorkTarget() : null;
        if (target == null) {
            positionProvider.clear();
            return false;
        }
        positionProvider.setTarget(target);
        return true;
    }

    @Override
    public InfoProvider getSensorInfo() {
        return positionProvider;
    }

}
