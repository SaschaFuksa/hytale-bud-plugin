package com.bud.system;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BudTimeInformation {

    public static TimeOfDay getTimeOfDay(Store<EntityStore> store) {
        LocalDateTime gameTime = readIngameDateTime(store);
        return getTimeOfDay(gameTime);
    }

    public static TimeOfDay getTimeOfDay(LocalDateTime gameTime) {
        if (gameTime == null) return TimeOfDay.DAY; // Fallback

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

    public static String getIngameDateTime(Store<EntityStore> store) {
        LocalDateTime gameTime = readIngameDateTime(store);
        return (gameTime != null) ? gameTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "Unknown";
    }


    private static LocalDateTime readIngameDateTime(Store<EntityStore> store) {
        WorldTimeResource wtr = (WorldTimeResource) store.getResource(WorldTimeResource.getResourceType());
        return wtr.getGameDateTime();
    }
    
}
