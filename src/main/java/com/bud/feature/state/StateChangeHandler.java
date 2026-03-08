package com.bud.feature.state;

import java.util.function.Consumer;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.StateSupport;

public class StateChangeHandler implements Consumer<StateChangeEvent> {

    @Override
    public void accept(StateChangeEvent event) {
        NPCEntity bud = event.bud();
        Role role = bud.getRole();
        if (role == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No role found for NPC: " + bud.getNPCTypeId());
            return;
        }

        StateSupport stateSupport = role.getStateSupport();
        int stateIndex = stateSupport.getStateHelper().getStateIndex(event.newState().getStateName());

        String defaultSubStateName = stateSupport.getStateHelper().getDefaultSubState();
        int subStateIndex = stateSupport.getStateHelper().getSubStateIndex(stateIndex,
                defaultSubStateName);

        stateSupport.setState(stateIndex, subStateIndex, true, false);

        MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
        markedSupport.setMarkedEntity("LockedTarget", event.owner().getReference());
        LoggerUtil.getLogger().fine(() -> "[BUD] Changed state to " + event.newState().getStateName() + " for NPC: " +
                bud.getNPCTypeId());
    }

}
