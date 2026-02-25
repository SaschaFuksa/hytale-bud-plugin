package com.bud.core.sound;

import javax.annotation.Nonnull;

import com.bud.core.types.BudState;

public class VeriSound implements IBudSound {

    @Override
    @Nonnull
    public String getDefensiveSound() {
        return "SFX_Fox_Alerted";
    }

    @Override
    @Nonnull
    public String getPassiveSound() {
        return "SFX_Fox_Alerted";
    }

    @Override
    @Nonnull
    public String getSittingSound() {
        return "SFX_Fox_Alerted";
    }

    @Override
    @Nonnull
    public String getSoundForState(BudState state) {
        return switch (state) {
            case PET_DEFENSIVE -> getDefensiveSound();
            case PET_PASSIVE -> getPassiveSound();
            case PET_SITTING -> getSittingSound();
            default -> "";
        };
    }

}
