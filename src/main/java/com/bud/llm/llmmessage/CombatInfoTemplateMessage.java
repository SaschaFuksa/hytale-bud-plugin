package com.bud.llm.llmmessage;

public class CombatInfoTemplateMessage extends AbstractYamlMessage {

    private String introduction;
    private String combatInfo;
    private String targetInfoTemplate;
    private String allyInfoTemplate;
    private String noInfoAvailable;

    public String getIntroduction() {
        return introduction;
    }

    public String getCombatInfo() {
        return combatInfo;
    }

    public String getTargetInfoTemplate() {
        return targetInfoTemplate;
    }

    public String getAllyInfoTemplate() {
        return allyInfoTemplate;
    }

    public String getNoInfoAvailable() {
        return noInfoAvailable;
    }

    public static CombatInfoTemplateMessage load(java.nio.file.Path path) {
        return loadFromFile(CombatInfoTemplateMessage.class, path);
    }
}
