package com.bud.feature.crafting;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.bud.core.config.ReactionConfig;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.feature.data.npc.BudRegistry;
import com.bud.feature.item.ItemMessage;
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

    private final ItemMessage itemPromptMessage = LLMPromptManager.getInstance().getItemPromptMessage();
    private final Map<String, String> benchKeywords;

    public UseBlockFilterSystem() {
        super(UseBlockEvent.Post.class);
        Map<String, String> bench = itemPromptMessage.getBench();
        benchKeywords = (bench != null) ? bench : Map.of();
        LoggerUtil.getLogger()
                .info(() -> "[BUD] UseBlockFilterSystem initialized with bench keywords: " + benchKeywords.keySet());
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
            if (!ReactionConfig.getInstance().isEnableCraftingReactions()) {
                return;
            }

            String blockTypeId = event.getBlockType().getId();
            String blockIdLower = blockTypeId.toLowerCase();
            System.out.println("UseBlockFilterSystem detected: " + blockTypeId);

            // Must contain "bench" to be relevant at all
            if (!blockIdLower.contains("bench")) {
                return;
            }

            // Find matching keyword (e.g. "cooking", "alchemy", "weapon")
            String matchedKeyword = null;
            for (String keyword : benchKeywords.keySet()) {
                if (blockIdLower.contains(keyword)) {
                    matchedKeyword = keyword;
                    break;
                }
            }
            if (matchedKeyword == null) {
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

            final String benchKey = matchedKeyword;
            LoggerUtil.getLogger().finer(() -> "[BUD] Bench Use Event: " + player.getDisplayName()
                    + " used bench=" + blockTypeId + " (keyword=" + benchKey + ")");

            RecentCraftCache.getInstance().add(playerId,
                    new CraftEntry(benchKey, CraftInteraction.USED));

        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in UseBlockFilterSystem: " + e.getMessage());
        }
    }
}
