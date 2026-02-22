package com.bud.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.components.PlayerBudComponent;
import com.bud.events.BudCreationEvent;
import com.bud.llm.orchestrator.MessageOrchestrator;
import com.bud.reaction.tracker.MoodTracker;
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
    @Nullable
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (addReason != AddReason.LOAD)
            return;
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null)
            return;
        PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent
                .getComponentType());
        if (playerBudComponent == null) {
            commandBuffer.addComponent(ref, PlayerBudComponent.getComponentType(), new PlayerBudComponent());
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Added PlayerBudComponent for player " + playerRef.getUsername());
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] PlayerBudComponent already exists for player " + playerRef.getUsername());
            BudCreationEvent.dispatch(ref, playerBudComponent.getBudTypes());
        }
        MoodTracker.getInstance().startPolling();
        MessageOrchestrator.getInstance().start();
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason removeReason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] PlayerJoinSystem detected player removal with reason: " + removeReason);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            MessageOrchestrator.getInstance().clearPlayer(playerRef.getUuid());
        }
    }

}
