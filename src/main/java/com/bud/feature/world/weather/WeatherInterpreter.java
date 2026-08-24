package com.bud.feature.world.weather;

import javax.annotation.Nonnull;

import com.bud.core.types.TimeOfDay;

public final class WeatherInterpreter {

    private static final String[] CLEAR_MARKERS = { "sunny", "clear" };

    private WeatherInterpreter() {
    }

    @Nonnull
    public static String resolveDisplayName(@Nonnull String weatherName, @Nonnull TimeOfDay timeOfDay) {
        if (timeOfDay != TimeOfDay.NIGHT && timeOfDay != TimeOfDay.MORNING) {
            return weatherName;
        }
        String lower = weatherName.toLowerCase();
        for (String marker : CLEAR_MARKERS) {
            if (lower.contains(marker)) {
                return "Clear";
            }
        }
        return weatherName;
    }
}
