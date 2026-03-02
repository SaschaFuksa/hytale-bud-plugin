package com.bud.feature.item;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.feature.LLMPromptManager;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class InventoryChangeListener implements Consumer<LivingEntityInventoryChangeEvent> {

    private final ItemMessage itemPromptMessage = LLMPromptManager.getInstance().getItemPromptMessage();

    private static Pattern RELEVANT_ITEMS_PATTERN;

    public InventoryChangeListener() {
        Map<String, String> inventory = itemPromptMessage.getInventory();
        String joined = String.join("|", inventory.keySet());
        RELEVANT_ITEMS_PATTERN = Pattern.compile(".*\\b(?i)(" + joined + ")\\b.*");
        LoggerUtil.getLogger().info(() -> "[BUD] InventoryChangeListener initialized with relevant item patterns: "
                + RELEVANT_ITEMS_PATTERN.pattern());
    }

    @Override
    public void accept(LivingEntityInventoryChangeEvent event) {
        try {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            Transaction transaction = event.getTransaction();

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
                        new ItemEntry(displayName, ItemInteraction.INVENTORY, budComponent));
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in InventoryChangeListener: " + e.getMessage());
        }
    }
}
