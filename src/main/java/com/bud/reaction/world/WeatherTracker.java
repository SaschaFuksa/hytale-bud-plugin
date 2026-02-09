package com.bud.reaction.world;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.interaction.InteractionManager;
import com.bud.llm.message.weather.LLMWeatherManager;
import com.bud.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class WeatherTracker {

    private static final WeatherTracker INSTANCE = new WeatherTracker();

    private final InteractionManager interactionManager = InteractionManager.getInstance();

    private WeatherTracker() {
    }

    private volatile ScheduledFuture<?> pollingTask;

    public static WeatherTracker getInstance() {
        return INSTANCE;
    }

    public synchronized void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        long interval = BudConfig.getInstance().getWeatherReactionPeriod();
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::checkWeather), 3L, interval,
                TimeUnit.SECONDS);
        LoggerUtil.getLogger().info(() -> "[BUD] Weather tracker started.");
    }

    public synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
            LoggerUtil.getLogger().fine(() -> "[BUD] Stopped weather polling task");
        }
    }

    private void checkWeather() {
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        if (owners.isEmpty()) {
            return;
        }
        interactionManager.processInteraction(owners, new LLMWeatherManager());
    }
}
