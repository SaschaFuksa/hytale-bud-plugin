package com.bud.core.types;

import javax.annotation.Nonnull;

public enum BudType {

    GRONKH("Gronkh_Bud"),
    KEYLETH("Keyleth_Bud"),
    VERI("Veri_Bud");

    private final @Nonnull String name;

    BudType(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String getName() {
        return name;
    }

}
