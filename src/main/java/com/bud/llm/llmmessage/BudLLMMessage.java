package com.bud.llm.llmmessage;

import java.nio.file.Path;
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
        if (states == null)
            return getFallback("default");

        String cleanState = state.toLowerCase().replace("pet", "").replace("sitting", "stay");
        String value = states.get(cleanState);
        if (value != null)
            return value;

        value = states.get(state.toLowerCase());
        if (value != null)
            return value;

        return getFallback("default");
    }

    public String getFallback(String key) {
        if (fallbacks == null)
            return "Og Og!";

        String cleanKey = key.toLowerCase().replace("pet", "").replace("sitting", "stay");
        String value = fallbacks.get(cleanKey);
        if (value != null)
            return value;

        value = fallbacks.get(key.toLowerCase());
        if (value != null)
            return value;

        return fallbacks.getOrDefault("default", "...");
    }

    public String getSystemPrompt() {
        return characteristics;
    }

    public String getPersonalWorldView() {
        return worldView;
    }

    public String getPersonalCombatView() {
        return combatView;
    }

    public static BudLLMMessage load(Path path) {
        return loadFromFile(BudLLMMessage.class, path);
    }
}
