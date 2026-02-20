package com.bud.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerJoinSystem extends RefSystem<EntityStore> {

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (addReason != AddReason.LOAD)
            return;
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null)
            return;
        ComponentType<EntityStore, PlayerBudComponent> componentType = PlayerBudComponent
                .getComponentType();
        if (componentType == null)
            return;
        PlayerBudComponent playerBudComponent = store.getComponent(ref, componentType);
        if (playerBudComponent == null) {
            commandBuffer.addComponent(ref, componentType, new PlayerBudComponent());
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Added PlayerBudComponent for player " + playerRef.getUsername());
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] PlayerBudComponent already exists for player " + playerRef.getUsername());
        }

    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Archetype.of(PlayerRef.getComponentType());
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> arg0, @Nonnull RemoveReason arg1,
            @Nonnull Store<EntityStore> arg2, @Nonnull CommandBuffer<EntityStore> arg3) {
    }

}
