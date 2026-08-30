package com.bud.feature.work;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

import com.bud.core.config.DebugConfig;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;

public final class WorkstationOrientation {

    private WorkstationOrientation() {
    }

    public static boolean spotsOnXAxis(@Nonnull World world, @Nonnull Vector3d anchor) {
        int stationX = (int) Math.floor(anchor.x);
        int stationY = (int) Math.floor(anchor.y) - 1;
        int stationZ = (int) Math.floor(anchor.z);
        int rotationIndex = WorldBlockSections.getRotationIndex(world, stationX, stationY, stationZ);
        RotationTuple rotation = RotationTuple.get(rotationIndex);
        Rotation yaw = rotation != null ? rotation.yaw() : Rotation.None;
        boolean onXAxis = yaw == Rotation.None || yaw == Rotation.OneEighty;
        if (DebugConfig.getInstance().isEnableBudDebugInfo()) {
            LoggerUtil.getLogger().info(() -> "[BUD] Workstation orientation at (" + stationX + "," + stationY + ","
                    + stationZ + ") - rotationIndex=" + rotationIndex + ", yaw=" + yaw + " -> spots on "
                    + (onXAxis ? "X" : "Z") + " axis.");
        }
        return onXAxis;
    }

}
