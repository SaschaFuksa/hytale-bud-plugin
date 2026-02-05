package com.bud.llm.llmmessage;

import java.util.Map;

public class WorldLLMMessage extends AbstractYamlMessage {

    private Map<String, String> zones;
    private Map<String, String> biomes;

    public String getZone(String zoneKey) {
        return zones != null ? zones.get(zoneKey) : null;
    }

    public String getBiome(String biomeKey) {
        return biomes != null ? biomes.get(biomeKey) : null;
    }

    public static WorldLLMMessage load(String path) {
        return loadFromResource(WorldLLMMessage.class, path);
    }

}
