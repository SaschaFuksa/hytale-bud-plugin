package com.bud.llm.messages;

import java.nio.file.Path;
import java.util.Map;

public class BudMessage extends AbstractYamlMessage {

    private String characteristics;
    private Map<String, String> states;
    private Map<String, String> fallbacks;
    private String worldView;
    private String combatView;
    private String blockView;
    private String weatherView;
    private String favoriteDayView;
    private String itemView;
    private String discoverView;
    private String craftView;
    private String teleportView;

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

        String value = fallbacks.get(key);
        if (value != null)
            return value;

        String cleanKey = key.toLowerCase().replace("pet", "").replace("sitting", "stay");
        value = fallbacks.get(cleanKey);
        if (value != null)
            return value;

        value = fallbacks.get(key.toLowerCase());
        if (value != null)
            return value;

        return fallbacks.getOrDefault("default", "...");
    }

    public String getPersonalWorldView() {
        return worldView;
    }

    public String getPersonalCombatView() {
        return combatView;
    }

    public String getPersonalBlockView() {
        return blockView;
    }

    public String getPersonalWeatherView() {
        return weatherView;
    }

    public String getFavoriteDayView() {
        return favoriteDayView;
    }

    public String getPersonalItemView() {
        return itemView;
    }

    public String getPersonalDiscoverView() {
        return discoverView;
    }

    public String getPersonalCraftView() {
        return craftView;
    }

    public String getPersonalTeleportView() {
        return teleportView;
    }

    public static BudMessage load(Path path) {
        return loadFromFile(BudMessage.class, path);
    }
}
