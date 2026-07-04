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
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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

            PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());

            String itemName = event.getItemStack().getItem().getId();
            if (itemName == null) {
                return;
            }
            String displayName = ItemUtil.getDisplayName(itemName);

            boolean relevantItem = RELEVANT_ITEMS_PATTERN.matcher(displayName).matches();

            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Player reference is null for player: " + playerRef.getUsername());
                return;
            }
            PlayerBudComponent playerBudComponent = ref.getStore().getComponent(ref,
                    PlayerBudComponent.getComponentType());
            BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
            if (relevantItem && budComponent != null) {
                LoggerUtil.getLogger()
                        .finer(() -> "[BUD] Inventory Change (ADD): " + playerRef.getUsername()
                                + " received " + displayName);
                RecentItemCache.getInstance().add(playerRef.getUsername(),
                        new ItemEntry(displayName, ItemInteraction.PICKUP, budComponent));
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in ItemPickupFilterSystem: " + e.getMessage());
        }
    }
}