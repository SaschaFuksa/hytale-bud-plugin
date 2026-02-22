package com.bud.reaction.combat;

import com.bud.queue.IQueueEntry;

public record OpponentEntry(String roleName, CombatState state) implements IQueueEntry {

    @Override
    public String getName() {
        return roleName;
    }

    public boolean isAttacked() {
        return state == CombatState.ATTACKED;
    }

    public boolean isWasAttacked() {
        return state == CombatState.WAS_ATTACKED;
    }

}
