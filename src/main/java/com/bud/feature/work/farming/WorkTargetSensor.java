package com.bud.feature.work.farming;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.corecomponents.SensorBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.hypixel.hytale.server.npc.sensorinfo.PositionProvider;

public class WorkTargetSensor extends SensorBase {

    @Nonnull
    private final PositionProvider positionProvider = new PositionProvider();

    private static boolean shouldLogDebug() {
        return true;
    }

    public WorkTargetSensor(@Nonnull BuilderWorkTargetSensor builder) {
        super(builder);
    }

    @Override
    public boolean matches(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, double dt,
            @Nonnull Store<EntityStore> store) {
        boolean debug = shouldLogDebug();
        if (!super.matches(ref, role, dt, store)) {
            if (debug) {
                LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] WorkTargetSensor.matches: "
                        + "super.matches() rejected (once/enabled/delay gate)");
            }
            positionProvider.clear();
            return false;
        }
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        Vector3d target = bud != null ? bud.getWorkTarget() : null;
        if (debug) {
            LoggerUtil.getLogger().warning(() -> "[BUD-TEMP-DEBUG] WorkTargetSensor.matches: called bud="
                    + (bud != null) + " workTarget=" + target);
        }
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
