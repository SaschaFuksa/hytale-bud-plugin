package com.bud.feature.work.reaction;

import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;

public class BuilderActionWorkTalk extends BuilderActionBase {

    @Override
    public String getShortDescription() {
        return "Fires a work-interrupt reaction when the player interacts with a Bud that is currently Working.";
    }

    @Override
    public String getLongDescription() {
        return "Enqueues a WorkEntry(INTERACT) on the Orchestrator PLAYER channel instead of cycling the "
                + "Bud's state - StateChangeSystem skips Working Buds entirely, so this is the only reaction "
                + "to a player interaction while Working.";
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
        return new WorkTalkAction(this, support);
    }

}
