package com.bud.reaction.tracker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.bud.config.ReactionConfig;
import com.bud.interaction.InteractionManager;
import com.bud.llm.messages.world.LLMWorldManager;
import com.bud.npc.BudRegistry;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.world.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.world.World;

public class WorldTracker extends AbstractTracker {

    private static final WorldTracker INSTANCE = new WorldTracker();

    private final InteractionManager interactionManager = InteractionManager.getInstance();

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
        BudRegistry budRegistry = BudRegistry.getInstance();
        if (budRegistry.getAllOwners().isEmpty()) {
            return;
        }
        long interval = ReactionConfig.getInstance().getWorldReactionPeriod();
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::triggerWorldMessage, interval,
                interval,
                TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] World tracker started.");
    }

    private void triggerWorldMessage() {
        BudRegistry budRegistry = BudRegistry.getInstance();
        if (budRegistry.getAllOwners().isEmpty()) {
            return;
        }
        Set<UUID> owners = budRegistry.getAllOwners();
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
                    Thread.ofVirtual().start(() -> {
                        IResult result = interactionManager.processInteraction(Set.of(owner),
                                new LLMWorldManager(weatherId));
                        if (!result.isSuccess()) {
                            result.printResult();
                        }
                    });
                });
            } catch (Exception e) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] World tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }

}
