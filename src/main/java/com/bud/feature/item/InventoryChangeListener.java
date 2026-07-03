package com.bud.feature.item;

import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class InventoryChangeListener extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

    private final ItemMessage itemPromptMessage = LLMPromptManager.getInstance().getItemPromptMessage();

    private static Pattern RELEVANT_ITEMS_PATTERN;

    public InventoryChangeListener() {
        super(InventoryChangeEvent.class);
    }

    public void setup() {
        if (RELEVANT_ITEMS_PATTERN != null) {
            return; // Already initialized
        }
        Map<String, String> inventory = itemPromptMessage.getInventory();
        String joined = String.join("|", inventory.keySet());
        RELEVANT_ITEMS_PATTERN = Pattern.compile(".*\\b(?i)(" + joined + ")\\b.*");
        LoggerUtil.getLogger().info(() -> "[BUD] InventoryChangeListener initialized with relevant item patterns: "
                + RELEVANT_ITEMS_PATTERN.pattern());
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> entityStore,
            @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent inventoryChangeEvent) {
        setup(); // Ensure the pattern is initialized
        try {
            PlayerRef entityRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (entityRef == null) {
                return;
            }

            Transaction transaction = inventoryChangeEvent.getTransaction();

            if (!(transaction instanceof ItemStackTransaction itemTransaction)) {
                return;
            }

            ActionType action = itemTransaction.getAction();
            if (action == null || !action.isAdd()) {
                return;
            }

            if (itemTransaction.getQuery() == null) {
                return;
            }
            ItemStack itemStack = itemTransaction.getQuery();
            if (itemStack == null) {
                return;
            }
            String itemName = itemStack.getItem().getId();
            if (itemName == null) {
                return;
            }
            String displayName = ItemUtil.getDisplayName(itemName);

            boolean relevantItem = RELEVANT_ITEMS_PATTERN.matcher(displayName).matches();
            Ref<EntityStore> playerRef = entityRef.getReference();
            if (playerRef == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Player reference is null for player: " + entityRef.getUsername());
                return;
            }
            PlayerBudComponent playerBudComponent = playerRef.getStore().getComponent(playerRef,
                    PlayerBudComponent.getComponentType());
            BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
            if (relevantItem && budComponent != null) {
                LoggerUtil.getLogger()
                        .finer(() -> "[BUD] Inventory Change (ADD): " + entityRef.getUsername()
                                + " received " + displayName);
                RecentItemCache.getInstance().add(entityRef.getUsername(),
                        new ItemEntry(displayName, ItemInteraction.INVENTORY, budComponent));
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in InventoryChangeListener: " + e.getMessage());
        }

    }

}
