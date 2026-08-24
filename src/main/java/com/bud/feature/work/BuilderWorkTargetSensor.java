package com.bud.feature.work;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.Feature;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderSensorBase;
import com.hypixel.hytale.server.npc.instructions.Sensor;

public class BuilderWorkTargetSensor extends BuilderSensorBase {

    @Override
    public String getShortDescription() {
        return "Matches while the acting Bud has a Workstation-assigned block to work on, exposing its position.";
    }

    @Override
    public String getLongDescription() {
        return "Reads BudComponent.getWorkTarget() (set by WorkstationFuelTickSystem, cleared by the acting "
                + "*WorkAction on success) and exposes it as this Instruction's InfoProvider position. Role-agnostic "
                + "- shared by FarmWorkAction and LumberingWorkAction alike. Does not search or range-check itself - "
                + "the Workstation already only ever assigns a target within WorkConfig.FieldRadius.";
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
        return new WorkTargetSensor(this);
    }

}
