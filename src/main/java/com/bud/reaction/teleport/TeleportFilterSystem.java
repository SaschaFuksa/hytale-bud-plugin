package com.bud.reaction.teleport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.npc.BudManager;
import com.bud.npc.BudRegistry;
import com.bud.result.IDataListResult;
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

/**
 * Filter system for detecting player teleportation.
 * 
 * Hytale uses ECS (Entity Component System) and when teleporting,
 * a Teleport component is briefly added to the entity and removed after
 * teleportation.
 * This system detects when the Teleport component is added or removed.
 */
public class TeleportFilterSystem extends RefChangeSystem<EntityStore, Teleport> {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final long TELEPORT_DELAY_MS = 25;

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        // Use Query.and(PlayerRef) to match only player entities
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
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component added to entity: " + player.getDisplayName());
        }
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
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Teleport component updated on entity: " + player.getDisplayName());
        }
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
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Teleport component removed from entity: " + player.getDisplayName());
        }
        try {
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef != null && BudRegistry.playerHasBud(playerRef.getUuid())) {
                // Delay teleport by 25ms to avoid conflicts with the player's teleport
                SCHEDULER.schedule(() -> {
                    store.getExternalData().getWorld().execute(() -> {
                        try {
                            IDataListResult<NPCEntity> result = BudManager.getInstance().teleportBuds(playerRef, store);
                            if (result.isSuccess()) {
                                LoggerUtil.getLogger()
                                        .fine(() -> "[BUD] Teleported Buds for player: " + playerRef.getUuid());
                            } else {
                                LoggerUtil.getLogger()
                                        .warning(() -> "[BUD] Failed to teleport Buds for player: "
                                                + playerRef.getUuid() +
                                                ". Reason: " + result.getMessage());
                                // TODO: Cleanup
                                // Create new
                            }
                        } catch (Exception e) {
                            LoggerUtil.getLogger()
                                    .severe(() -> "[BUD] Error in delayed Bud teleport: " + e.getMessage());
                        }
                    });
                }, TELEPORT_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Error in TeleportFilterSystem.onComponentRemoved: " + e.getMessage());
        }
    }
}
