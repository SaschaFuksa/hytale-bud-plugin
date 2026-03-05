package com.bud.feature.world.weather;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.feature.AbstractTracker;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.world.WorldInformationUtil;
import com.bud.feature.world.WorldResolver;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
                this::triggerWeatherMessage, interval, interval,
                TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] Weather tracker started.");
    }

    private void triggerWeatherMessage() {
        Set<PlayerRef> players = BudManager.getInstance().getTrackedPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (PlayerRef playerRef : players) {
            if (!playerRef.isValid()) {
                LoggerUtil.getLogger().warning(() -> "[BUD] Invalid player reference encountered.");
                BudManager.getInstance().unregisterPlayer(playerRef);
                continue;
            }
            World world = WorldResolver.resolveStrict(playerRef).orElse(null);
            if (world == null) {
                continue;
            }

            try {
                world.execute(() -> {
                    Ref<EntityStore> playerEntityRef = playerRef.getReference();
                    if (playerEntityRef == null) {
                        return;
                    }
                    Store<EntityStore> entityStore = playerEntityRef.getStore();
                    PlayerBudComponent playerComponent = entityStore.getComponent(playerEntityRef,
                            PlayerBudComponent.getComponentType());
                    if (playerComponent == null || !playerComponent.hasBuds()) {
                        return;
                    }

                    BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerComponent);
                    if (budComponent == null) {
                        LoggerUtil.getLogger().warning(() -> "[BUD] No BudComponent found for player: "
                                + playerRef.getUsername());
                        return;
                    }

                    Weather weather = WorldInformationUtil.getCurrentWeather(playerRef);
                    if (weather == null) {
                        return;
                    }
                    String weatherId = weather.getId();
                    if (weatherId == null) {
                        return;
                    }
                    if (!playerComponent.updateWeatherIfChanged(weatherId)) {
                        return;
                    }

                    IQueueEntry entry = new WeatherEntry(weatherId, budComponent);
                    Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                            OrchestratorChannel.AMBIENT,
                            entry,
                            "weather",
                            playerRef.getUsername(),
                            new LLMInteractionEntry(LLMWeatherMessageCreation.getInstance(),
                                    entry),
                            System.currentTimeMillis()));
                });
            } catch (Exception e) {
                LoggerUtil.getLogger().warning(
                        () -> "[BUD] Weather tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }
}
