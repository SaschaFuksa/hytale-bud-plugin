package com.bud.feature.state;

import java.util.function.Consumer;

import com.bud.core.BudExecutionSupport;
import com.bud.core.types.BudState;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.StateSupport;

public class StateChangeHandler implements Consumer<StateChangeEvent> {

    @Override
    public void accept(StateChangeEvent event) {
        NPCEntity bud = event.bud();
        boolean applied = BudExecutionSupport.with(bud, support -> {
            StateSupport stateSupport = support.getStateSupport();
            int stateIndex = stateSupport.getStateHelper().getStateIndex(event.newState().getStateName());

            String defaultSubStateName = stateSupport.getStateHelper().getDefaultSubState();
            int subStateIndex = stateSupport.getStateHelper().getSubStateIndex(stateIndex,
                    defaultSubStateName);

            stateSupport.setState(stateIndex, subStateIndex, true, false);

            if (event.newState() != BudState.WORKING) {
                MarkedEntitySupport markedSupport = support.getMarkedEntitySupport();
                markedSupport.setMarkedEntity("LockedTarget", event.owner().getReference());
            }
        });
        if (!applied) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No role found for NPC: " + bud.getNPCTypeId());
            return;
        }
        LoggerUtil.getLogger().fine(() -> "[BUD] Changed state to " + event.newState().getStateName() + " for NPC: " +
                bud.getNPCTypeId());
    }

}
