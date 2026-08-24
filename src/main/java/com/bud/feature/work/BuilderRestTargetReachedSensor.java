package com.bud.feature.work;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;

public class BuilderRestTargetReachedSensor extends BuilderSensorBase {

    @Override
    public String getShortDescription() {
        return "Matches once the acting Bud's own position is close enough to its RestTarget.";
    }

    @Override
    public String getLongDescription() {
        return "Compares TransformComponent.getPosition() against BudComponent.getRestTarget(): the horizontal "
                + "distance must be within 0.6 blocks and the height difference within 1.5. Horizontal and "
                + "vertical are checked separately so that standing next to a RestTarget that sits one block "
                + "higher (restPosition ON_STATION) does not count as having arrived.";
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public Builder<Sensor> readConfig(JsonElement json) {
        return this;
    }

    @Override
    public Sensor build(BuilderSupport support) {
        return new RestTargetReachedSensor(this);
    }

}
