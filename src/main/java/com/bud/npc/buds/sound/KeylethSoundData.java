package com.bud.npc.buds.sound;

public class KeylethSoundData implements IBudSoundData {

    @Override
    public String getAttackSound() {
        return "SFX_Kweebec_Plushie_Impact";
    }

    @Override
    public String getPassiveSound() {
        return "SFX_Kweebec_Plushie_Impact";
    }

    @Override
    public String getIdleSound() {
        return "SFX_Kweebec_Plushie_Impact";
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
