package com.bud.reaction.teleport;

import javax.annotation.Nonnull;

import com.bud.npc.BudManager;
import com.bud.npc.BudRegistry;
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

/**
 * Filter system for detecting player teleportation.
 * 
 * Hytale uses ECS (Entity Component System) and when teleporting,
 * a Teleport component is briefly added to the entity and removed after
 * teleportation.
 * This system detects when the Teleport component is added or removed.
 */
public class TeleportFilterSystem extends RefChangeSystem<EntityStore, Teleport> {

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        // Use Query.any() to match all entities, then filter for players in the handler
        // Player.getComponentType() returns null during system registration
        return Query.and(PlayerRef.getComponentType());
    }

    @Override
    @Nonnull
    public ComponentType<EntityStore, Teleport> componentType() {
        return Teleport.getComponentType();
    }

    /**
     * Called when the Teleport component is added to an entity.
     * This indicates the entity is about to be teleported.
     */
    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Teleport component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component added to entity: "
                + store.getComponent(ref, Player.getComponentType()).getDisplayName());
    }

    /**
     * Called when the Teleport component is updated (set) on an entity.
     */
    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            Teleport oldComponent,
            @Nonnull Teleport newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component updated on entity: "
                + store.getComponent(ref, Player.getComponentType()).getDisplayName());
    }

    /**
     * Called when the Teleport component is removed from an entity.
     * This indicates the teleportation has completed.
     */
    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Teleport component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component removed from entity: "
                + store.getComponent(ref, Player.getComponentType()).getDisplayName());
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef != null && BudRegistry.playerHasBud(playerRef.getUuid())) {
                BudManager.getInstance().teleportBuds(playerRef, store);
                LoggerUtil.getLogger().fine(() -> "[BUD] Teleported Buds for player: " + playerRef.getUuid());
            }
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Error in TeleportFilterSystem.onComponentRemoved: " + e.getMessage());
        }
    }
}
