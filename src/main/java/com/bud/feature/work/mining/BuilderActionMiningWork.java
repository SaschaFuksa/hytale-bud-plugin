package com.bud.feature.work.mining;

import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionMiningWork extends BuilderActionBase {

    @Override
    public String getShortDescription() {
        return "Performs the acting Bud's currently assigned mining work (dig/mine) at the position provided by "
                + "the preceding Sensor.";
    }

    @Override
    public String getLongDescription() {
        return "Reads BudComponent.getWorkType() to either dig a new growth site (DIG) or mine a ready one (MINE) "
                + "- kept separate from FarmWorkAction/LumberingWorkAction since Mining tracks growth via its own "
                + "OreGrowthBlock component rather than the shared Till/Plant/Water/Fertilize steps, see "
                + "docs/bud-worker-mode-plan.md, \"Mining Konzept final\".";
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
        return new MiningWorkAction(this, support);
    }

}
