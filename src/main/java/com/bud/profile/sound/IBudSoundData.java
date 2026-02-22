package com.bud.profile.sound;

import javax.annotation.Nonnull;

public interface IBudSoundData {

    @Nonnull
    String getAttackSound();

    @Nonnull
    String getPassiveSound();

    @Nonnull
    String getIdleSound();

    /**
     * Get the sound for a specific state.
     * 
     * @param state The state name (e.g., "PetDefensive", "PetPassive",
     *              "PetSitting")
     * @return The sound id
     */
    @Nonnull
    String getSoundForState(String state);

}
