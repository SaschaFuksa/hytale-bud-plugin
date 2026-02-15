package com.bud.reaction.item;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bud.llm.message.prompt.ItemPromptMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
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

    private final ItemPromptMessage itemPromptMessage = LLMPromptManager.getInstance().getItemPromptMessage();

    private static Pattern RELEVANT_ITEMS_PATTERN;

    public ItemPickupFilterSystem() {
        super(InteractivelyPickupItemEvent.class);
        Map<String, String> inventory = itemPromptMessage.getInventory();
        String joined = inventory.keySet().stream()
                .map(key -> key.replace("_", " "))
                .collect(Collectors.joining("|"));
        RELEVANT_ITEMS_PATTERN = Pattern.compile(".*\\b(?i)(" + joined + ")\\b.*");
        LoggerUtil.getLogger().info(() -> "[BUD] ItemPickupFilterSystem initialized with relevant item patterns: "
                + RELEVANT_ITEMS_PATTERN.pattern());
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

            Player player = store.getComponent(entityRef, Player.getComponentType());

            String itemName = event.getItemStack().getItem().getId();
            String displayName = ItemUtil.getDisplayName(itemName);

            boolean relevantItem = RELEVANT_ITEMS_PATTERN.matcher(displayName).matches();
            UUID playerId = player.getUuid();

            // Only care if the player has a Bud
            if (relevantItem && BudRegistry.playerHasBud(playerId)) {
                LoggerUtil.getLogger()
                        .finer(() -> "[BUD] Item Pickup Event: " + player.getDisplayName() + " picked up "
                                + displayName);
                RecentItemCache.getInstance().add(playerId,
                        new ItemEntry(displayName, getPriority(displayName), ItemInteraction.PICKUP));
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in ItemPickupFilterSystem: " + e.getMessage());
        }
    }

    private static int getPriority(String itemName) {
        // Simple priority logic based on item type keywords
        if (itemName.contains("gem")) {
            return 3;
        } else if (itemName.contains("ingot")) {
            return 2;
        } else if (itemName.contains("ore")) {
            return 1;
        }
        return 0; // Default priority for other items
    }
}