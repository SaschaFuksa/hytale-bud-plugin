package com.bud.feature.work;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.components.BudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.work.reaction.LLMWorkMessageCreation;
import com.bud.feature.work.reaction.WorkEntry;
import com.bud.feature.work.reaction.WorkReactionKind;
import com.bud.llm.interaction.LLMInteractionEntry;

public final class WorkstationOutputFullReactions {

    private WorkstationOutputFullReactions() {
    }

    public static void fireIfDue(@Nonnull WorkstationBlockEntity workstation, @Nullable String blockedItemId) {
        if (!ReactionConfig.getInstance().isEnableWorkReactions()) {
            return;
        }
        long cooldownMillis = ReactionConfig.getInstance().getOutputFullReactionPeriodSeconds() * 1000L;
        if (!workstation.isOutputFullReactionDue(cooldownMillis)) {
            return;
        }
        BudComponent boundBud = workstation.getBoundBud();
        if (boundBud == null) {
            return;
        }
        workstation.markOutputFullReactionFired();

        WorkEntry workEntry = new WorkEntry(boundBud, WorkReactionKind.OUTPUT_FULL, boundBud.getWorkType(),
                blockedItemId);
        LLMInteractionEntry entry = new LLMInteractionEntry(LLMWorkMessageCreation.getInstance(), workEntry);
        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                OrchestratorChannel.ACTIVITY,
                workEntry,
                "workOutputFull",
                boundBud.getPlayerRef().getUsername(),
                entry,
                System.currentTimeMillis()));
    }

}
