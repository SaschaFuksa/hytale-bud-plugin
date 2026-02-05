package com.bud.llm.llmmessage;

import java.util.Map;
import java.nio.file.Path;

public class ZoneLLMMessage extends AbstractYamlMessage {

    private String zone;
    private Map<String, String> biomes;

    public String getZone() {
        return zone;
    }

    public Map<String, String> getBiomes() {
        return biomes;
    }

    public static ZoneLLMMessage load(Path path) {
        return loadFromFile(ZoneLLMMessage.class, path);
    }
}
