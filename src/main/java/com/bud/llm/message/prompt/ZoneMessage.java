package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.Map;

public class ZoneMessage extends BaseYamlMessage {

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
