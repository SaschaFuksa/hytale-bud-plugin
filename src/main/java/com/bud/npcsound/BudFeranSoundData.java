package com.bud.npcsound;

public class BudFeranSoundData implements IBudNPCSoundData {

    @Override
    public String getAttackSound() {
        return "SFX_Fox_Hurt";
    }

    @Override
    public String getPassiveSound() {
        return "SFX_Fox_Alerted";
    }

    @Override
    public String getIdleSound() {
        return "SFX_Fox_Sleep";
    }

    @Override
    public String getSoundForState(String state) {
        return switch (state) {
            case "PetDefensive" -> getAttackSound();
            case "PetPassive" -> getPassiveSound();
            case "PetSitting" -> getIdleSound();
            case "Idle" -> getIdleSound();
            default -> null;
        };
    }

}
