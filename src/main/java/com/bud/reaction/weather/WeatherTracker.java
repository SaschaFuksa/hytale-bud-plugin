package com.bud.reaction.weather;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.interaction.InteractionManager;
import com.bud.llm.message.weather.LLMWeatherManager;
import com.bud.npc.BudRegistry;
import com.bud.util.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

public class WeatherTracker {

    private static final WeatherTracker INSTANCE = new WeatherTracker();
    private final Map<UUID, String> lastWeather = new HashMap<>();
    private ScheduledFuture<?> pollingTask;

    private WeatherTracker() {
    }

    public static WeatherTracker getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        // Poll every 5 seconds
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::checkWeather, 5, 5,
                TimeUnit.SECONDS);
        LoggerUtil.getLogger().info(() -> "[BUD] Weather tracker started.");
    }

    public void clearPlayer(UUID ownerId) {
        lastWeather.remove(ownerId);
    }

    private void checkWeather() {
        try {
            Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
            Set<UUID> changedOwners = new HashSet<>();

            for (UUID ownerId : owners) {
                PlayerRef player = Universe.get().getPlayerByUuid(ownerId);
                if (player == null)
                    continue;

                World world = player.getWorld();
                if (world == null)
                    continue;

                Weather weather = WorldInformationUtil.getCurrentWeather(world, player);
                if (weather == null)
                    continue;

                String weatherId = weather.getId();
                String previous = lastWeather.get(ownerId);

                if (previous != null && !previous.equals(weatherId)) {
                    changedOwners.add(ownerId);
                    LoggerUtil.getLogger()
                            .info(() -> "[BUD] Weather changed for " + ownerId + ": " + previous + " -> " + weatherId);
                }
                lastWeather.put(ownerId, weatherId);
            }

            if (!changedOwners.isEmpty()) {
                InteractionManager.getInstance().processInteraction(changedOwners, new LLMWeatherManager());
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in weather tracking: " + e.getMessage());
        }
    }
}
