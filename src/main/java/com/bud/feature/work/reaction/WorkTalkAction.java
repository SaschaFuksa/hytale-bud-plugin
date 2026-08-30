package com.bud.feature.work.reaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.components.BudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.core.types.BudState;
import com.bud.core.types.WorkType;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

public class WorkTalkAction extends ActionBase {

    public WorkTalkAction(@Nonnull BuilderActionWorkTalk builder, @Nonnull BuilderSupport support) {
        super(builder);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull ExecutionSupport executionSupport,
            @Nullable InfoProvider infoProvider, double dt, @Nonnull Store<EntityStore> store) {
        if (!super.canExecute(ref, executionSupport, infoProvider, dt, store) || !ReactionConfig.getInstance()
                .isEnableWorkReactions()) {
            return false;
        }
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        return bud != null && bud.getCurrentState() == BudState.WORKING;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull ExecutionSupport executionSupport,
            @Nullable InfoProvider infoProvider, double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, executionSupport, infoProvider, dt, store);
        BudComponent bud = store.getComponent(ref, BudComponent.getComponentType());
        if (bud == null) {
            return true;
        }
        WorkType workType = bud.getWorkType();
        WorkEntry workEntry = new WorkEntry(bud, WorkReactionKind.INTERACT, workType, null);
        LLMInteractionEntry entry = new LLMInteractionEntry(LLMWorkMessageCreation.getInstance(), workEntry);
        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                OrchestratorChannel.PLAYER,
                workEntry,
                "workInteract",
                bud.getPlayerRef().getUsername(),
                entry,
                System.currentTimeMillis()));
        return true;
    }

}
