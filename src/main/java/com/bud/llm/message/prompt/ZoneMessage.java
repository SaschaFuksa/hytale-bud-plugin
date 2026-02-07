package com.bud.llm.message.prompt;

import java.util.Map;
import java.nio.file.Path;

public class ZoneMessage extends AbstractYamlMessage {

    private String zone;
    private Map<String, String> biomes;

    public String getZone() {
        return zone;
    }

    public Map<String, String> getBiomes() {
        return biomes;
    }

    public static ZoneMessage load(Path path) {
        return loadFromFile(ZoneMessage.class, path);
    }
}
