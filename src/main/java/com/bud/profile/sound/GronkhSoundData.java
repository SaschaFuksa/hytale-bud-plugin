package com.bud.profile.sound;

import javax.annotation.Nonnull;

public class GronkhSoundData implements IBudSoundData {

    @Override
    @Nonnull
    public String getDefensiveSound() {
        return "SFX_Trork_Exertion";
    }

    @Override
    @Nonnull
    public String getPassiveSound() {
        return "SFX_Trork_Alerted";
    }

    @Override
    @Nonnull
    public String getSittingSound() {
        return "SFX_Trork_Alerted";
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
