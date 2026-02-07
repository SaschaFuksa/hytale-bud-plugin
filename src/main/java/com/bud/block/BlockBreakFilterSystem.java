package com.bud.block;

import java.util.UUID;

import com.bud.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Filter system for block breaking events.
 * Captures when players break blocks to provide context for Buds.
 */
public class BlockBreakFilterSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public BlockBreakFilterSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {
        try {
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);

            // Try to see if the entity breaking the block is a player
            Player player = store.getComponent(entityRef, Player.getComponentType());

            if (player != null) {
                UUID playerId = player.getUuid();

                // Only care if the player has a Bud
                if (playerHasBud(playerId)) {
                    String blockName = event.getBlockType().getId();
                    // Normalize block name if needed (e.g. hytale:dirt -> dirt)
                    if (blockName.contains(":")) {
                        blockName = blockName.split(":")[1];
                    }

                    RecentBlockCache.addBlock(playerId, blockName);

                    LoggerUtil.getLogger().finer(() -> "[BUD] Block Break Event: " + player.getDisplayName() + " broke "
                            + event.getBlockType().getId());
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in BlockBreakFilterSystem: " + e.getMessage());
        }
    }

    private boolean playerHasBud(UUID playerId) {
        return !BudRegistry.getInstance().getByOwner(playerId).isEmpty();
    }
}
