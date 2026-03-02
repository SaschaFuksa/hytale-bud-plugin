package com.bud.feature.world;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.core.types.TimeOfDay;
import com.bud.feature.AbstractTracker;
import com.bud.feature.LLMInteractionManager;
import com.bud.feature.world.env.LLMWorldContext;
import com.bud.feature.world.env.LLMWorldMessageCreation;
import com.bud.feature.world.time.TimeInformationUtil;
import com.bud.feature.world.weather.LLMWeatherContext;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

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
                    TimeOfDay timeOfDay = TimeInformationUtil.getTimeOfDay();
                    Zone zone = WorldInformationUtil.getCurrentZone(world,
                            playerComponent.getPlayerRef().getTransform().getPosition());
                    Biome biome = WorldInformationUtil.getCurrentBiome(world,
                            playerComponent.getPlayerRef().getTransform().getPosition());
                    Weather weather = WorldInformationUtil.getCurrentWeather(playerComponent.getPlayerRef());
                    String weatherId = weather != null ? weather.getId() : "unknown";
                    LLMWeatherContext weatherContext = LLMWeatherContext.from(weatherId, budComponent);
                    Thread.ofVirtual().start(() -> {
                        LLMInteractionEntry entry = new LLMInteractionEntry(
                                LLMWorldMessageCreation.getInstance(),
                                new LLMWorldContext(timeOfDay, zone, biome, weatherContext, budComponent));
                        interactionManager.processInteraction(entry);
                    });
                });
            } catch (Exception e) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] World tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }

}
