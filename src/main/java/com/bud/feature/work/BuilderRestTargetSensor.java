package com.bud.feature.work;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;

public class BuilderRestTargetSensor extends BuilderSensorBase {

    @Override
    public String getShortDescription() {
        return "Matches while the acting Bud has a resting-position target to walk to, exposing its position.";
    }

    @Override
    public String getLongDescription() {
        return "Reads BudComponent.getRestTarget() (set by WorkstationFuelTickSystem when a Bud enters the "
                + ".Resting sub-state, based on the Bud's BudDefinition.getRestPosition()) and exposes it as this "
                + "Instruction's InfoProvider position. Mirrors WorkTargetSensor's shape exactly.";
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public Builder<Sensor> readConfig(JsonElement json) {
        provideFeature(Feature.Position);
        return this;
    }

    @Override
    public Sensor build(BuilderSupport support) {
        return new RestTargetSensor(this);
    }

}
