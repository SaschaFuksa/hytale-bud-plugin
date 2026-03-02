package com.bud.feature.teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class TeleportFilterSystem extends RefChangeSystem<EntityStore, Teleport> {

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType());
    }

    @Override
    @Nonnull
    public ComponentType<EntityStore, Teleport> componentType() {
        return Teleport.getComponentType();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Teleport component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component added to entity: " + player.getDisplayName());
        }
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nullable Teleport oldComponent,
            @Nonnull Teleport newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component updated on entity: " + player.getDisplayName());
        }
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Teleport component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component removed on entity: " + player.getDisplayName());
        }
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent.getComponentType());

            if (playerRef != null && playerBudComponent.hasBuds()) {
                List<BudComponent> budComponents = new ArrayList<>();
                for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
                    budComponents.add(BudManager.getInstance().getBudComponent(bud));
                }
                if (!budComponents.isEmpty()) {
                    int speakingIndex = ThreadLocalRandom.current().nextInt(budComponents.size());
                    for (int index = 0; index < budComponents.size(); index++) {
                        BudComponent budComponent = budComponents.get(index);
                        if (budComponent != null) {
                            TeleportEvent.dispatch(store, budComponent, index == speakingIndex);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Error in TeleportFilterSystem.onComponentRemoved: " + e.getMessage());
        }

    }
}
