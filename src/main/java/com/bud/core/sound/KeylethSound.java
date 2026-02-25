package com.bud.core.sound;

import javax.annotation.Nonnull;

import com.bud.core.types.BudState;

public class KeylethSound implements IBudSound {

    @Override
    @Nonnull
    public String getDefensiveSound() {
        return "SFX_Kweebec_Plushie_Impact";
    }

    @Override
    @Nonnull
    public String getPassiveSound() {
        return "SFX_Kweebec_Plushie_Impact";
    }

    @Override
    @Nonnull
    public String getSittingSound() {
        return "SFX_Kweebec_Plushie_Impact";
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
