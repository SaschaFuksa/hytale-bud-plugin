package com.bud.llmmessage;

public interface ILLMBudNPCMessage {

    String getSystemPrompt();

    String getAttackMessage();

    String getPassiveMessage();

    String getIdleMessage();

    String getPersonalWorldView();
    
    /**
     * Get the LLM prompt for a specific state.
     * @param state The state name (e.g., "PetDefensive", "PetPassive", "PetSitting")
     * @return The prompt to send to the LLM, or null if no prompt is defined
     */
    String getPromptForState(String state);
    
    /**
     * Get the fallback message for a state when LLM is not available.
     * @param state The state name
     * @return The fallback message, or null if no message is defined
     */
    String getFallbackMessage(String state);
}
