package com.bud.reaction.tracker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.llm.message.weather.LLMWeatherManager;
import com.bud.npc.BudRegistry;
import com.bud.orchestrator.MessageChannel;
import com.bud.orchestrator.MessageOrchestrator;
import com.bud.orchestrator.QueuedEvent;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.world.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.world.World;

public class WeatherTracker extends BaseTracker {

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
        BudRegistry budRegistry = BudRegistry.getInstance();
        if (budRegistry.getAllOwners().isEmpty()) {
            return;
        }
        long interval = BudConfig.getInstance().getWeatherReactionPeriod();
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::triggerWeatherMessage), interval, interval,
                TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] Weather tracker started.");
    }

    private void triggerWeatherMessage() {
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        if (owners.isEmpty()) {
            return;
        }

        for (UUID owner : owners) {
            PlayerInstance playerInstance = PlayerRegistry.getInstance().getByOwner(owner);
            if (playerInstance == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] No PlayerInstance found for owner: " + owner);
                continue;
            }
            World world = WorldInformationUtil.resolveWorld(playerInstance.getPlayerRef());
            if (world == null)
                continue;
            try {
                world.execute(() -> {
                    Weather weather = WorldInformationUtil.getCurrentWeather(playerInstance.getPlayerRef());
                    String weatherId = weather != null ? weather.getId() : "unknown";
                    MessageOrchestrator.getInstance().enqueue(new QueuedEvent(
                            MessageChannel.AMBIENT, 2, "weather",
                            new LLMWeatherManager(weatherId), owner, System.currentTimeMillis()));
                });
            } catch (Exception e) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Weather tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }
}
