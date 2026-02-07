package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.Map;

public class BudMessage extends AbstractYamlMessage {

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
            return "...";

        // 1. Try exact match (e.g. "worldView")
        String value = fallbacks.get(key);
        if (value != null)
            return value;

        // 2. Try cleaned/lowercase match (e.g. "worldview" or "stay")
        String cleanKey = key.toLowerCase().replace("pet", "").replace("sitting", "stay");
        value = fallbacks.get(cleanKey);
        if (value != null)
            return value;

        // 3. Try pure lowercase
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

    public static BudMessage load(Path path) {
        return loadFromFile(BudMessage.class, path);
    }
}
