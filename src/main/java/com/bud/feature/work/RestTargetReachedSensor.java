package com.bud.feature.work;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

public class RestTargetReachedSensor extends SensorBase {

    private static final double REACHED_HORIZONTAL_RANGE = 0.6;

    private static final double REACHED_VERTICAL_RANGE = 1.5;

    public RestTargetReachedSensor(@Nonnull BuilderRestTargetReachedSensor builder) {
        super(builder);
    }

    @Override
    public boolean matches(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, double dt,
            @Nonnull Store<EntityStore> store) {
        if (!super.matches(ref, role, dt, store)) {
            return false;
        }
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        Vector3d target = bud != null ? bud.getRestTarget() : null;
        if (target == null) {
            return false;
        }
        ComponentType<EntityStore, TransformComponent> transformType = TransformComponent.getComponentType();
        if (transformType == null) {
            return false;
        }
        TransformComponent transform = store.getComponent(ref, transformType);
        if (transform == null) {
            return false;
        }
        Vector3d position = transform.getPosition();
        double dx = position.x - target.x;
        double dz = position.z - target.z;
        return dx * dx + dz * dz <= REACHED_HORIZONTAL_RANGE * REACHED_HORIZONTAL_RANGE
                && Math.abs(position.y - target.y) <= REACHED_VERTICAL_RANGE;
    }

    @Override
    public InfoProvider getSensorInfo() {
        return null;
    }

}
