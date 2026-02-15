package com.bud.reaction.combat;

import com.bud.reaction.ICacheEntry;

public record OpponentEntry(String roleName, CombatState state) implements ICacheEntry {

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
