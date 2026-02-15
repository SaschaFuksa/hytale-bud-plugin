package com.bud.reaction.crafting;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.bud.BudConfig;
import com.bud.llm.message.prompt.ItemPromptMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Filter system for UseBlockEvent.Post.
 * Detects when players use processing benches (cooking, alchemy, etc.)
 * and adds USED entries to the RecentCraftCache.
 */
public class UseBlockFilterSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {

    private final ItemPromptMessage itemPromptMessage = LLMPromptManager.getInstance().getItemPromptMessage();
    private final Set<String> benchBlockIds;

    public UseBlockFilterSystem() {
        super(UseBlockEvent.Post.class);
        Map<String, String> bench = itemPromptMessage.getBench();
        benchBlockIds = (bench != null) ? bench.keySet() : Set.of();
        LoggerUtil.getLogger().info(() -> "[BUD] UseBlockFilterSystem initialized with bench IDs: " + benchBlockIds);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull UseBlockEvent.Post event) {
        try {
            if (!BudConfig.getInstance().isEnableCraftingReactions()) {
                return;
            }

            String blockTypeId = event.getBlockType().getId();

            // Check if the block is a known bench type
            if (!benchBlockIds.contains(blockTypeId)) {
                return;
            }

            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
            Player player = store.getComponent(entityRef, Player.getComponentType());

            if (player == null) {
                return;
            }

            UUID playerId = player.getUuid();
            if (!BudRegistry.playerHasBud(playerId)) {
                return;
            }

            LoggerUtil.getLogger().finer(() -> "[BUD] Bench Use Event: " + player.getDisplayName()
                    + " used bench=" + blockTypeId);

            RecentCraftCache.getInstance().add(playerId,
                    new CraftEntry(blockTypeId, CraftInteraction.USED));

        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in UseBlockFilterSystem: " + e.getMessage());
        }
    }
}
