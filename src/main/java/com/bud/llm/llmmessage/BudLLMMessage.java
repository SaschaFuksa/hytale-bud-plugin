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
        if (states != null) {
            String value = states.get(state.toLowerCase());
            if (value != null) {
                return value;
            }
        }
        return getFallback("state");
    }

    public String getFallback(String key) {
        return fallbacks != null ? fallbacks.get(key) : null;
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
