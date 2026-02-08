package com.bud.player;

public class PlayerInstance {

    private String lastKnownWeather;

    public PlayerInstance(String lastKnownWeather) {
        this.lastKnownWeather = lastKnownWeather;
    }

    public String getLastKnownWeather() {
        return lastKnownWeather;
    }

    public void setLastKnownWeather(String lastKnownWeather) {
        this.lastKnownWeather = lastKnownWeather;
    }

}
