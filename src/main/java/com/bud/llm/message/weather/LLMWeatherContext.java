package com.bud.llm.message.weather;

import com.bud.llm.message.creation.IPromptContext;

public record LLMWeatherContext(String weatherName) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        if ("weatherName".equals(contextId)) {
            return this.weatherName;
        }
        return null;
    }

    public static LLMWeatherContext from(String weatherId) {
        final String weatherName = getWeatherName(weatherId);
        return new LLMWeatherContext(weatherName);
    }

    public String getWeatherInformation() {
        return "The current weather is: " + this.weatherName + ".";
    }

    private static String getWeatherName(String weatherId) {
        weatherId = weatherId.replace("Zone0", "");
        weatherId = weatherId.replace("Zone1", "");
        weatherId = weatherId.replace("Zone2", "");
        weatherId = weatherId.replace("Zone3", "");
        weatherId = weatherId.replace("Zone4", "");
        return weatherId.replace("_", " ");
    }

}
