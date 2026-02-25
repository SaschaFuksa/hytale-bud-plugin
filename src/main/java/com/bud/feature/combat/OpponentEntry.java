package com.bud.feature.combat;

import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.feature.queue.IQueueEntry;

public record OpponentEntry(String roleName, CombatState state) implements IQueueEntry {

    public boolean isAttacked() {
        return state == CombatState.ATTACKED;
    }

    public boolean isWasAttacked() {
        return state == CombatState.WAS_ATTACKED;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPriority'");
    }

    @Override
    public LLMInteractionEntry getInteractionEntry() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInteractionEntry'");
    }

}
