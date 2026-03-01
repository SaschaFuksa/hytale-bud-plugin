package com.bud.feature.world.weather;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.core.BudManager;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.feature.AbstractTracker;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.world.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.world.World;

public class WeatherTracker extends AbstractTracker {

    private static final WeatherTracker INSTANCE = new WeatherTracker();

    private WeatherTracker() {
    }

    public static WeatherTracker getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling()) {
            return;
        }
        long interval = ReactionConfig.getInstance().getWeatherReactionPeriod();
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::triggerWeatherMessage), interval, interval,
                TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] Weather tracker started.");
    }

    private void triggerWeatherMessage() {
        Set<PlayerBudComponent> players = BudManager.getInstance().getAllPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (PlayerBudComponent playerComponent : players) {
            World world = WorldInformationUtil.resolveWorld(playerComponent.getPlayerRef());
            if (world == null)
                continue;
            try {
                world.execute(() -> {
                    Weather weather = WorldInformationUtil.getCurrentWeather(playerComponent.getPlayerRef());
                    String weatherId = weather != null ? weather.getId() : "unknown";
                    Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                            OrchestratorChannel.AMBIENT,
                            2,
                            "weather",
                            playerComponent.getPlayerRef().getUsername(),
                            System.currentTimeMillis()));
                });
            } catch (Exception e) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Weather tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }
}
