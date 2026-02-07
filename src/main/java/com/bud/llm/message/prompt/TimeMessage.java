package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TimeMessage extends AbstractYamlMessage {

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

    public Map<String, String> getTimes() {
        Map<String, String> times = new HashMap<>();
        times.put("morning", morning);
        times.put("day", day);
        times.put("afternoon", afternoon);
        times.put("evening", evening);
        times.put("night", night);
        return times;
    }

    public static TimeMessage load(Path path) {
        return loadFromFile(TimeMessage.class, path);
    }
}
