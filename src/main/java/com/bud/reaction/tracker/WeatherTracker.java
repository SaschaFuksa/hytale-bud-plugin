package com.bud.reaction.tracker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.interaction.InteractionManager;
import com.bud.llm.message.weather.LLMWeatherManager;
import com.bud.npc.BudRegistry;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.world.WorldInformationUtil;
import com.bud.result.IResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;

public class WeatherTracker extends AbstractTracker {

    private static final WeatherTracker INSTANCE = new WeatherTracker();

    private final InteractionManager interactionManager = InteractionManager.getInstance();

    private WeatherTracker() {
    }

    private volatile ScheduledFuture<?> pollingTask;

    public static WeatherTracker getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        BudRegistry budRegistry = BudRegistry.getInstance();
        if (budRegistry.getAllOwners().isEmpty()) {
            return;
        }
        long interval = BudConfig.getInstance().getWeatherReactionPeriod();
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::triggerWeatherMessage), 3L, interval,
                TimeUnit.SECONDS);
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
            Weather weather = WorldInformationUtil.getCurrentWeather(playerInstance.getPlayerRef());
            Thread.ofVirtual().start(() -> {
                IResult result = interactionManager.processInteraction(Set.of(owner),
                        new LLMWeatherManager(weather));
                if (!result.isSuccess()) {
                    result.printResult();
                }
            });
        }
    }
}
