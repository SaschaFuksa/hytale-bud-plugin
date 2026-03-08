package com.bud.feature.world.env;

import java.nio.file.Path;
import java.util.Map;

import com.bud.llm.messages.AbstractYamlMessage;

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
