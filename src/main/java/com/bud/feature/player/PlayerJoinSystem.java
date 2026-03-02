package com.bud.feature.player;

import javax.annotation.Nonnull;

import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.debug.BudDebugInfo;
import com.bud.core.types.BudType;
import com.bud.feature.bud.MoodTracker;
import com.bud.feature.queue.creation.BudCreationEntry;
import com.bud.feature.queue.creation.BudCreationQueue;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
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
        PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent
                .getComponentType());
        if (playerBudComponent == null) {
            commandBuffer.addComponent(ref, PlayerBudComponent.getComponentType(), new PlayerBudComponent(playerRef));
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Added PlayerBudComponent for player " + playerRef.getUsername());
        } else {
            playerBudComponent.setPlayerRef(playerRef);
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] PlayerBudComponent already exists for player " + playerRef.getUsername());
            for (BudType budType : playerBudComponent.getBudTypes()) {
                BudCreationQueue.getInstance()
                        .addToCache(new BudCreationEntry(ref, budType));
            }
        }
        MoodTracker.getInstance().startPolling();
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
            Orchestrator.getInstance().clearPlayer(playerRef.getUsername());
        }
    }

}
