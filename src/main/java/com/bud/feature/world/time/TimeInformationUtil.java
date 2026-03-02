package com.bud.feature.world.time;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import com.bud.core.types.DayOfWeek;
import com.bud.core.types.TimeOfDay;
import com.bud.feature.world.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TimeInformationUtil {

    @Nonnull
    public static TimeOfDay getTimeOfDay(Store<EntityStore> store) {
        LocalDateTime gameTime = readIngameDateTime(store);
        TimeOfDay timeOfDay = getTimeOfDay(gameTime);
        if (timeOfDay == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] time of day is null, defaulting to DAY.");
            return TimeOfDay.DAY;
        }
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] In-game time: " + gameTime + ", determined time of day: " + timeOfDay);
        return timeOfDay;
    }

    private static TimeOfDay getTimeOfDay(LocalDateTime gameTime) {
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
            World world = WorldInformationUtil.getDefaultWorld();
            if (world == null) {
                return DayOfWeek.MONDAY;
            }
            EntityStore entityStore = world.getEntityStore();
            return getDayOfWeek(entityStore.getStore());
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Failed to read in-game time for day of week calculation: " + e.getMessage());
            return DayOfWeek.MONDAY;
        }
    }

    private static DayOfWeek getDayOfWeek(Store<EntityStore> store) {
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
