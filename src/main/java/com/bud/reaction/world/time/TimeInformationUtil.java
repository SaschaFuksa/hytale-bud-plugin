package com.bud.reaction.world.time;

import java.time.LocalDateTime;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TimeInformationUtil {

    public static TimeOfDay getTimeOfDay() {
        try {
            return getTimeOfDay(Universe.get().getDefaultWorld().getEntityStore().getStore());
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Failed to read in-game time for day of week calculation: " + e.getMessage());
            return TimeOfDay.DAY;
        }
    }

    public static TimeOfDay getTimeOfDay(Store<EntityStore> store) {
        LocalDateTime gameTime = readIngameDateTime(store);
        return getTimeOfDay(gameTime);
    }

    public static TimeOfDay getTimeOfDay(LocalDateTime gameTime) {
        if (gameTime == null)
            return TimeOfDay.DAY; // Fallback

        int hour = gameTime.getHour();

        if (hour >= 5 && hour < 10) {
            return TimeOfDay.MORNING;
        } else if (hour >= 10 && hour < 14) {
            return TimeOfDay.DAY;
        } else if (hour >= 14 && hour < 18) {
            return TimeOfDay.AFTERNOON;
        } else if (hour >= 18 && hour < 22) {
            return TimeOfDay.EVENING;
        } else {
            return TimeOfDay.NIGHT;
        }
    }

    public static DayOfWeek getDayOfWeek() {
        try {
            return getDayOfWeek(Universe.get().getDefaultWorld().getEntityStore().getStore());
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Failed to read in-game time for day of week calculation: " + e.getMessage());
            return DayOfWeek.MONDAY;
        }
    }

    public static DayOfWeek getDayOfWeek(Store<EntityStore> store) {
        LocalDateTime gameTime = readIngameDateTime(store);
        if (gameTime == null) {
            return DayOfWeek.MONDAY;
        }
        int dayValue = gameTime.getDayOfWeek().getValue();
        LoggerUtil.getLogger().finer(
                () -> "[BUD] In-game day of week value: " + dayValue + " (" + DayOfWeek.values()[dayValue - 1] + ")");
        return DayOfWeek.values()[dayValue - 1];
    }

    private static LocalDateTime readIngameDateTime(Store<EntityStore> store) {
        WorldTimeResource wtr = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
        return wtr.getGameDateTime();
    }

}
