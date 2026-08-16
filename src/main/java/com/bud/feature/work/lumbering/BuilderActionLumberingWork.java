package com.bud.feature.work.lumbering;

import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionLumberingWork extends BuilderActionBase {

    @Override
    public String getShortDescription() {
        return "Performs the acting Bud's currently assigned lumbering work (fell) at the position provided by "
                + "the preceding Sensor.";
    }

    @Override
    public String getLongDescription() {
        return "Reads BudComponent.getWorkType()/getPendingFellBlockPosition() to fell the assigned tree at the "
                + "real block position - kept separate from FarmWorkAction since Lumbering acts on a walkable "
                + "neighbor of the target block rather than the block itself, see docs/bud-worker-mode-plan.md, "
                + "\"Lumbering Slice C\".";
    }

    @Override
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Override
    public Action build(BuilderSupport support) {
        if (support == null) {
            throw new IllegalArgumentException("BuilderSupport cannot be null");
        }
        return new LumberingWorkAction(this, support);
    }

}
