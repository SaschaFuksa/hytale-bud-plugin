package com.bud.feature.world;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.feature.AbstractTracker;
import com.bud.feature.LLMInteractionManager;
import com.bud.feature.world.env.LLMWorldMessageCreation;
import com.bud.feature.world.env.WorldEntry;
import com.bud.feature.world.weather.WeatherEntry;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WorldTracker extends AbstractTracker {

    private static final WorldTracker INSTANCE = new WorldTracker();

    private static final LLMInteractionManager interactionManager = LLMInteractionManager.getInstance();

    private WorldTracker() {
    }

    public static WorldTracker getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling()) {
            return;
        }
        long interval = ReactionConfig.getInstance().getWorldReactionPeriod();
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::triggerWorldMessage, interval,
                interval,
                TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] World tracker started.");
    }

    private void triggerWorldMessage() {
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
                    String weatherId = weather != null ? weather.getId() : "unknown";
                    WeatherEntry weatherEntry = new WeatherEntry(weatherId, budComponent);
                    if (entityStore == null) {
                        LoggerUtil.getLogger().warning(() -> "[BUD] Entity store is null for player: "
                                + playerRef.getUsername());
                        return;
                    }
                    WorldEntry worldEntry = WorldEntry.from(playerRef, world,
                            entityStore,
                            weatherEntry, budComponent);
                    if (worldEntry == null) {
                        LoggerUtil.getLogger().warning(() -> "[BUD] Could not create WorldEntry for player: "
                                + playerRef.getUsername());
                        return;
                    }
                    Thread.ofVirtual().start(() -> {
                        LLMInteractionEntry entry = new LLMInteractionEntry(
                                LLMWorldMessageCreation.getInstance(),
                                worldEntry);
                        interactionManager.processInteraction(entry);
                    });
                });
            } catch (Exception e) {
                LoggerUtil.getLogger().warning(
                        () -> "[BUD] World tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }

}
