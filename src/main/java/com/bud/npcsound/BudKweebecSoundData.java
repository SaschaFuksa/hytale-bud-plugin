package com.bud.npcsound;

public class BudKweebecSoundData implements IBudNPCSoundData {

    @Override
    public String getAttackSound() {
        return "SFX_Kweebec_Exertion";
    }

    @Override
    public String getPassiveSound() {
        return "SFX_Kweebec_Alerted";
    }

    @Override
    public String getIdleSound() {
        return "SFX_Kweebec_Search";
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
