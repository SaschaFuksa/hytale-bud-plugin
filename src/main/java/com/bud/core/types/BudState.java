package com.bud.core.types;

import javax.annotation.Nonnull;

public enum BudState {

    PET_DEFENSIVE("PetDefensive"),
    PET_PASSIVE("PetPassive"),
    PET_SITTING("PetSitting");

    private final String stateName;

    BudState(String stateName) {
        this.stateName = stateName;
    }

    @Nonnull
    public String getStateName() {
        if (stateName == null) {
            throw new IllegalStateException("State name cannot be null");
        }
        return stateName;
    }

    public static BudState fromStateName(String stateName) {
        for (BudState state : values()) {
            if (state.getStateName().equals(stateName)) {
                return state;
            }
        }
        return null;
    }

}
