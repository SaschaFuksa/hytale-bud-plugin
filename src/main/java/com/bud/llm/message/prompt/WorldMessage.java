package com.bud.llm.message.prompt;

public class WorldMessage extends AbstractYamlMessage {

    private String environmentInfo;

    public String getEnvironmentInfo() {
        return environmentInfo;
    }

    public static WorldMessage load(java.nio.file.Path path) {
        return loadFromFile(WorldMessage.class, path);
    }
}
