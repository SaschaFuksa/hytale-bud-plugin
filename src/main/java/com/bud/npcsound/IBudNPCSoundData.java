package com.bud.npcsound;

public interface IBudNPCSoundData {

    String getAttackSound();

    String getPassiveSound();

    String getIdleSound();

    /**
     * Get the sound for a specific state.
     * @param state The state name (e.g., "PetDefensive", "PetPassive", "PetSitting")
     * @return The sound id
     */
    String getSoundForState(String state);
    
}
