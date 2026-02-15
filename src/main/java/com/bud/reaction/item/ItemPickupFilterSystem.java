package com.bud.reaction.item;

import java.util.UUID;

import com.bud.npc.BudRegistry;
import com.bud.reaction.ItemUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Filter system for item pickup events.
 * Captures when players pick up items to provide context for Buds.
 */
public class ItemPickupFilterSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    public ItemPickupFilterSystem() {
        super(InteractivelyPickupItemEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull InteractivelyPickupItemEvent event) {
        try {
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);

            // Try to see if the entity picking up the item is a player
            Player player = store.getComponent(entityRef, Player.getComponentType());
            System.out.println("ItemPickupFilterSystem: Player " + (player != null ? player.getDisplayName() : "null")
                    + " picked up item " + ItemUtil.getItemName(event.getItemStack().getItem().getId()));
            // [SOUT] ItemPickupFilterSystem: Player Vaerith picked up item Plant Flower
            // Here: Pickups like rare mushroom, flowers and berries
            // Common White2
            if (player != null) {
                UUID playerId = player.getUuid();

                // Only care if the player has a Bud
                if (BudRegistry.playerHasBud(playerId)) {
                    String itemName = event.getItemStack().getItem().getId();

                    LoggerUtil.getLogger()
                            .finer(() -> "[BUD] Item Pickup Event: " + player.getDisplayName() + " picked up "
                                    + itemName);
                    // RecentItemCache.addItem(playerId, itemName);
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in ItemPickupFilterSystem: " + e.getMessage());
        }
    }
}