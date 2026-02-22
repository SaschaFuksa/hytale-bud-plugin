package com.bud.llm.messages.weather;

import com.bud.components.BudComponent;
import com.bud.llm.messages.IPromptContext;
import com.bud.profile.IBudProfile;

public record LLMWeatherContext(String weatherName) implements IPromptContext {

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

    @Override
    public BudComponent getBudComponent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudComponent'");
    }

    @Override
    public IBudProfile getBudProfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudProfile'");
    }

}
