package com.bud.feature.work.farming;

import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionFarmWork extends BuilderActionBase {

    @Override
    public String getShortDescription() {
        return "Performs the acting Bud's currently assigned farming work (till/plant/water/harvest) at the "
                + "position provided by the preceding Sensor.";
    }

    @Override
    public String getLongDescription() {
        return "Reads BudComponent.getWorkType() to decide which farming step to perform at the target "
                + "position (till/plant/water; harvest is detection-only in Phase 6) - one Action instead of "
                + "several JSON Actions per Instruction, see docs/bud-worker-mode-plan.md, \"Phase 5 — Drei "
                + "tragende Regeln für Phase 6-10\".";
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
        return new FarmWorkAction(this, support);
    }

}
