package com.bud.player;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerInstance {

    private final PlayerRef playerRef;

    private String lastKnownWeather;

    public PlayerInstance(PlayerRef playerRef, String lastKnownWeather) {
        this.playerRef = playerRef;
        this.lastKnownWeather = lastKnownWeather;
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

}
