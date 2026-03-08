package com.bud.feature.player;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.config.ReactionConfig;
import com.bud.core.debug.BudDebugInfo;
import com.bud.core.types.BudType;
import com.bud.feature.bud.MoodTracker;
import com.bud.feature.queue.creation.BudCreationEntry;
import com.bud.feature.queue.creation.BudCreationQueue;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.world.WorldInformationUtil;
import com.bud.feature.world.WorldTracker;
import com.bud.feature.world.weather.WeatherTracker;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerJoinSystem extends RefSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }

    @Override
    @SuppressWarnings("null")
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null)
            return;
        BudManager.getInstance().registerPlayer(playerRef);
        PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent
                .getComponentType());
        if (playerBudComponent == null) {
            PlayerBudComponent newPlayerBudComponent = new PlayerBudComponent(playerRef);
            initializeWeatherBaseline(playerRef, newPlayerBudComponent);
            commandBuffer.addComponent(ref, PlayerBudComponent.getComponentType(), newPlayerBudComponent);
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Added PlayerBudComponent for player " + playerRef.getUsername());
        } else {
            playerBudComponent.setPlayerRef(playerRef);
            initializeWeatherBaseline(playerRef, playerBudComponent);
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] PlayerBudComponent already exists for player " + playerRef.getUsername());
            for (BudType budType : playerBudComponent.getBudTypes()) {
                BudCreationQueue.getInstance()
                        .addToCache(new BudCreationEntry(ref, budType));
            }
        }
        MoodTracker.getInstance().startPolling();
        if (ReactionConfig.getInstance().isEnableWorldReactions()) {
            WorldTracker.getInstance().startPolling();
        }
        if (ReactionConfig.getInstance().isEnableWeatherReactions()) {
            WeatherTracker.getInstance().startPolling();
        }
        Orchestrator.getInstance().start();
        if (DebugConfig.getInstance().isEnablePlayerInfo()) {
            BudDebugInfo.getInstance().logPlayerInfo(playerRef, store);
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason removeReason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] PlayerJoinSystem detected player removal with reason: " + removeReason);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            BudManager.getInstance().unregisterPlayer(playerRef);
            Orchestrator.getInstance().clearPlayer(playerRef.getUsername());
        }
    }

    public static void initializeWeatherBaseline(@Nonnull PlayerRef playerRef,
            @Nonnull PlayerBudComponent playerBudComponent) {
        Weather weather = WorldInformationUtil.getCurrentWeather(playerRef);
        if (weather == null || weather.getId() == null || weather.getId().isBlank()) {
            return;
        }
        playerBudComponent.setLastKnownWeatherId(weather.getId());
    }

}
