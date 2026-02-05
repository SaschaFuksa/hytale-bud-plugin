package com.bud.llm.llmmessage;

public class WorldInfoTemplateMessage extends AbstractYamlMessage {

    private String introduction;
    private String environmentInfo;

    public String getIntroduction() {
        return introduction;
    }

    public String getEnvironmentInfo() {
        return environmentInfo;
    }

    public static WorldInfoTemplateMessage load(java.nio.file.Path path) {
        return loadFromFile(WorldInfoTemplateMessage.class, path);
    }
}
