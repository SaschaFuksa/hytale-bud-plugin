package com.bud.reaction.state;

import javax.annotation.Nonnull;

public enum BudState {

    PET_DEFENSIVE("PetDefensive"),
    PET_PASSIVE("PetPassive"),
    PET_SITTING("PetSitting");

    private final String stateName;

    BudState(String stateName) {
        this.stateName = stateName;
    }

    public String getStateName() {
        return stateName;
    }

    @Nonnull
    public static BudState fromStateName(String stateName) {
        for (BudState state : values()) {
            if (state.getStateName().equals(stateName)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown state name: " + stateName);
    }

}
