package com.bud.feature.item;

public enum ItemInteraction {

    PICKUP("pickup"),
    INVENTORY("inventory");

    private final String action;

    ItemInteraction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
