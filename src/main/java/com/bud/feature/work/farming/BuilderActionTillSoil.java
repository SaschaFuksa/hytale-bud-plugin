package com.bud.feature.work.farming;

import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionTillSoil extends BuilderActionBase {

    @Override
    public String getShortDescription() {
        return "Till the block at the position provided by the preceding Sensor, if it is a Bud's Workstation field.";
    }

    @Override
    public String getLongDescription() {
        return "Changes the block found by the preceding Sensor (e.g. a native Block Sensor against a "
                + "tillable-soil BlockSet) to Soil_Dirt_Tilled, if it is within the acting Bud's "
                + "WorkConfig.FieldRadius of its bound Workstation. No native Action performs this "
                + "transform (SetBlockToPlace/PlaceBlock requires an empty placement target, ChangeBlock "
                + "interactions are player-tool-only) - see docs/bud-worker-mode-plan.md.";
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public Action build(BuilderSupport support) {
        return new TillSoilAction(this, support);
    }

}
