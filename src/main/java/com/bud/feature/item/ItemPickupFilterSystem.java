package com.bud.feature.item;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.feature.LLMPromptManager;
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

public class ItemPickupFilterSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    private final ItemMessage itemPromptMessage = LLMPromptManager.getInstance().getItemPromptMessage();

    private static Pattern RELEVANT_ITEMS_PATTERN;

    public ItemPickupFilterSystem() {
        super(InteractivelyPickupItemEvent.class);
        Map<String, String> pickup = itemPromptMessage.getPickup();
        String joined = pickup.keySet().stream()
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
            if (itemName == null) {
                return;
            }
            String displayName = ItemUtil.getDisplayName(itemName);

            boolean relevantItem = RELEVANT_ITEMS_PATTERN.matcher(displayName).matches();

            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Player reference is null for player: " + player.getDisplayName());
                return;
            }
            PlayerBudComponent playerBudComponent = playerRef.getStore().getComponent(playerRef,
                    PlayerBudComponent.getComponentType());
            BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
            if (relevantItem && budComponent != null) {
                LoggerUtil.getLogger()
                        .finer(() -> "[BUD] Inventory Change (ADD): " + player.getDisplayName()
                                + " received " + displayName);
                RecentItemCache.getInstance().add(player.getDisplayName(),
                        new ItemEntry(displayName, ItemInteraction.PICKUP, budComponent));
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in ItemPickupFilterSystem: " + e.getMessage());
        }
    }
}