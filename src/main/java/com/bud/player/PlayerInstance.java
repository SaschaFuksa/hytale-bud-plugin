package com.bud.player;

import com.bud.reaction.world.time.DayOfWeek;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerInstance {

    private final PlayerRef playerRef;

    private String lastKnownWeather;

    private DayOfWeek currentDay;

    public PlayerInstance(PlayerRef playerRef, String lastKnownWeather, DayOfWeek currentDay) {
        this.playerRef = playerRef;
        this.lastKnownWeather = lastKnownWeather;
        this.currentDay = currentDay;
    }

    public PlayerRef getPlayerRef() {
        return playerRef;
    }

    public String getLastKnownWeather() {
        return lastKnownWeather;
    }

    public void setLastKnownWeather(String lastKnownWeather) {
        this.lastKnownWeather = lastKnownWeather;
    }

    public DayOfWeek getLastDay() {
        return currentDay;
    }

    public void setLastDay(DayOfWeek currentDay) {
        this.currentDay = currentDay;
    }

}
