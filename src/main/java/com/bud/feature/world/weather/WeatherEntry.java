package com.bud.feature.world.weather;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.queue.IQueueEntry;

public record WeatherEntry(@Nonnull String weatherName, @Nonnull BudComponent budComponent) implements IQueueEntry {

    public WeatherEntry {
        weatherName = cleanWeatherName(weatherName);
    }

    @Nonnull
    public String getWeatherInformation() {
        return "The current weather is: " + this.weatherName + ".";
    }

    @Nonnull
    private static String cleanWeatherName(String weatherId) {
        weatherId = weatherId.replace("Zone0", "");
        weatherId = weatherId.replace("Zone1", "");
        weatherId = weatherId.replace("Zone2", "");
        weatherId = weatherId.replace("Zone3", "");
        weatherId = weatherId.replace("Zone4", "");
        weatherId = weatherId.replace("_", " ");
        if (weatherId.isBlank()) {
            return "Clear";
        }
        return weatherId;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Override
    @Nonnull
    public String getEntryName() {
        return weatherName;
    }

}
