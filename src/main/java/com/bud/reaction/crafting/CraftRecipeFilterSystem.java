package com.bud.reaction.crafting;

import javax.annotation.Nonnull;

import com.bud.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Filter system for CraftRecipeEvent.Post.
 * Captures when players craft items to provide context for Buds.
 */
public class CraftRecipeFilterSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    public CraftRecipeFilterSystem() {
        super(CraftRecipeEvent.Post.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull CraftRecipeEvent.Post event) {
        try {
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
            Player player = store.getComponent(entityRef, Player.getComponentType());

            if (player != null) {
                UUID playerId = player.getUuid();
                if (BudRegistry.playerHasBud(playerId)) {
                    String itemId = event.getCraftedRecipe().getPrimaryOutput().getItemId();

                    LoggerUtil.getLogger().finer(() -> "[BUD] Craft Recipe Event: " + player.getDisplayName()
                            + " crafted item=" + itemId);

                    RecentCraftCache.getInstance().add(playerId, new CraftEntry(itemId));
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in CraftRecipeFilterSystem: " + e.getMessage());
        }
    }
}
