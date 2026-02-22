package com.bud.profile.sound;

import javax.annotation.Nonnull;

public class KeylethSoundData implements IBudSoundData {

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
    public String getSoundForState(String state) {
        return switch (state) {
            case "PetDefensive" -> getDefensiveSound();
            case "PetPassive" -> getPassiveSound();
            case "PetSitting" -> getSittingSound();
            default -> "";
        };
    }

}
