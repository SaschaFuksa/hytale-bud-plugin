package com.bud.feature.world.weather;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMWeatherContext(String weatherName, BudComponent budComponent) implements IPromptContext {

    public static LLMWeatherContext from(String weatherId, BudComponent budComponent) {
        final String weatherName = getWeatherName(weatherId);
        return new LLMWeatherContext(weatherName, budComponent);
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
        return this.budComponent;
    }

    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
    }

}
