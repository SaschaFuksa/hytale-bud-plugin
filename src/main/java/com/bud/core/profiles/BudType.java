package com.bud.core.profiles;

public enum BudType {

    GRONKH("Gronkh_Bud"),
    KEYLETH("Keyleth_Bud"),
    VERI("Veri_Bud");

    private final String name;

    BudType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
