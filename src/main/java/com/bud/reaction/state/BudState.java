package com.bud.reaction.state;

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

}
