package com.bud.reaction.block;

import java.util.UUID;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Filter system for block placing events.
 * Captures when players place blocks to provide context for Buds.
 */
public class BlockPlaceFilterSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public BlockPlaceFilterSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull PlaceBlockEvent event) {
        try {
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);

            // Try to see if the entity placing the block is a player
            Player player = store.getComponent(entityRef, Player.getComponentType());

            if (player != null) {
                UUID playerId = player.getUuid();

                // Only care if the player has a Bud
                if (BlockUtil.playerHasBud(playerId)) {
                    String blockName = BlockUtil.getBlockName(event.getItemInHand().getItem().getBlockId());

                    LoggerUtil.getLogger()
                            .finer(() -> "[BUD] Block Place Event: " + player.getDisplayName() + " placed "
                                    + blockName);
                    RecentBlockCache.addBlock(playerId, blockName, BlockInteraction.PLACE);
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in BlockPlaceFilterSystem: " + e.getMessage());
        }
    }

}
