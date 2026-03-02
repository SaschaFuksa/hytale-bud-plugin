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
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
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
        World defaultWorld = WorldInformationUtil.getDefaultWorld();
        if (defaultWorld == null) {
            return;
        }

        defaultWorld.execute(() -> {
            Set<PlayerBudComponent> players = BudManager.getInstance().getAllPlayers();
            if (players.isEmpty()) {
                return;
            }

            for (PlayerBudComponent playerComponent : players) {
                World world = WorldInformationUtil.resolveWorld(playerComponent.getPlayerRef());
                if (world == null)
                    continue;
                BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerComponent);
                if (budComponent == null) {
                    LoggerUtil.getLogger().warning(() -> "[BUD] No BudComponent found for player: "
                            + playerComponent.getPlayerRef().getUsername());
                    continue;
                }
                try {
                    world.execute(() -> {
                        Weather weather = WorldInformationUtil.getCurrentWeather(playerComponent.getPlayerRef());
                        String weatherId = weather != null ? weather.getId() : "unknown";
                        WeatherEntry weatherEntry = new WeatherEntry(weatherId, budComponent);
                        Store<EntityStore> entityStore = world.getEntityStore().getStore();
                        if (entityStore == null) {
                            LoggerUtil.getLogger().warning(() -> "[BUD] Entity store is null for player: "
                                    + playerComponent.getPlayerRef().getUsername());
                            return;
                        }
                        WorldEntry worldEntry = WorldEntry.from(playerComponent.getPlayerRef(), world,
                                entityStore,
                                weatherEntry, budComponent);
                        if (worldEntry == null) {
                            LoggerUtil.getLogger().warning(() -> "[BUD] Could not create WorldEntry for player: "
                                    + playerComponent.getPlayerRef().getUsername());
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
        });
    }

}
