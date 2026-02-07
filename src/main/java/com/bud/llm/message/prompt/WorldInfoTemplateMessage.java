package com.bud.llm.message.prompt;

public class WorldInfoTemplateMessage extends AbstractYamlMessage {

    private String environmentInfo;

    public String getEnvironmentInfo() {
        return environmentInfo;
    }

    public static WorldInfoTemplateMessage load(java.nio.file.Path path) {
        return loadFromFile(WorldInfoTemplateMessage.class, path);
    }
}
