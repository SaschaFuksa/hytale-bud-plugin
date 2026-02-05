package com.bud.llm.llmmessage;

public class TimeLLMMessage extends AbstractYamlMessage {

    private String morning;
    private String day;
    private String afternoon;
    private String evening;
    private String night;

    public String getMorning() {
        return morning;
    }

    public String getDay() {
        return day;
    }

    public String getAfternoon() {
        return afternoon;
    }

    public String getEvening() {
        return evening;
    }

    public String getNight() {
        return night;
    }

    public static TimeLLMMessage load(String path) {
        return loadFromResource(TimeLLMMessage.class, path);
    }
}
