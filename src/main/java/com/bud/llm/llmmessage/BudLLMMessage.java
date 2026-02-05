package com.bud.llm.llmmessage;

import java.util.Map;

public class BudLLMMessage extends AbstractYamlMessage {

    private String characteristics;
    private Map<String, String> states;
    private Map<String, String> fallbacks;
    private String worldView;
    private String combatView;

    public String getCharacteristics() {
        return characteristics;
    }

    public String getState(String state) {
        return states != null ? states.get(state) : null;
    }

    public String getFallback(String state) {
        return fallbacks != null ? fallbacks.get(state) : null;
    }

    public String getWorldView() {
        return worldView;
    }

    public String getCombatView() {
        return combatView;
    }

    public static BudLLMMessage load(String path) {
        return loadFromResource(BudLLMMessage.class, path);
    }
}
