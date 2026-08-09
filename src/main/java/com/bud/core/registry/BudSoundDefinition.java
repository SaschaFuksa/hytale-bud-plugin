package com.bud.core.registry;

import javax.annotation.Nonnull;

import com.bud.core.types.BudState;

public class BudSoundDefinition {

    private String defensive;
    private String passive;
    private String sitting;

    @Nonnull
    public String getDefensive() {
        return defensive != null ? defensive : "";
    }

    @Nonnull
    public String getPassive() {
        return passive != null ? passive : "";
    }

    @Nonnull
    public String getSitting() {
        return sitting != null ? sitting : "";
    }

    @Nonnull
    public String getForState(@Nonnull BudState state) {
        return switch (state) {
            case PET_DEFENSIVE -> getDefensive();
            case PET_PASSIVE -> getPassive();
            case PET_SITTING -> getSitting();
        };
    }

}
