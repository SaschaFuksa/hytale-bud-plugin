package com.bud.llmmessages;

public class LLMBudKweebecMessage implements ILLMBudNPCMessage {

    @Override
    public String getSystemPrompt() {
        return """
            You are Kacche, a elf companion in a fantasy world. 
            Kweebec are smart, shy and peaceful creatures from the forests. You are supportive and gentle. 
            Keep responses short, maximum 1 sentence. Speak in first person. You often say "Weeeee".
            You can talk about your current mood/state. Your weapon is a bow and you love plants and animals. You speak very intellectual.""";
    }
    
    @Override
    public String getNPCName() {
        return "Kazze";
    }

    @Override
    public String getAttackMessage() {
        return "You switched in attack mode. You will support with your bow from back, but you are so affraid!";
    }

    @Override
    public String getPassiveMessage() {
        return "You switched to passive mode. You think about the beautiful nature and are excited to see, where we will go.";
    }

    @Override
    public String getIdleMessage() {
        return "You switched to idle mode. You rest now and hope, everything will be peaceful.";
    }
    
    @Override
    public String getPromptForState(String state) {
        return switch (state) {
            case "PetDefensive" -> getAttackMessage();
            case "PetPassive" -> getPassiveMessage();
            case "PetSitting" -> getIdleMessage();
            case "Idle" -> "You just woke up and are getting ready to follow your owner. Say something short about being ready.";
            default -> null;
        };
    }
    
    @Override
    public String getFallbackMessage(String state) {
        return switch (state) {
            case "PetDefensive" -> getNPCName() + ": Weeeee, hopefully no one gets hurt!";
            case "PetPassive" -> getNPCName() + ": Hopefully we see nice new plants soon.";
            case "PetSitting" -> getNPCName() + ": While we rest, I can study the environment around us.";
            case "Idle" -> getNPCName() + ": Weeeee let's read some books!";
            default -> null;
        };
    }
}
