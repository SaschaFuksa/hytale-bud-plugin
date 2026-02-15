package com.bud.reaction.item;

import java.util.UUID;
import java.util.function.Consumer;

import com.bud.npc.BudRegistry;
import com.bud.reaction.ItemUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.transaction.ActionType;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;

/**
 * Listener for inventory change events.
 * Captures when items are added to a player's inventory (e.g. automatic ore
 * pickup).
 * Unlike InteractivelyPickupItemEvent (which requires pressing "F"),
 * this fires for ALL inventory additions including auto-pickup.
 */
public class InventoryChangeListener implements Consumer<LivingEntityInventoryChangeEvent> {

    @Override
    public void accept(LivingEntityInventoryChangeEvent event) {
        try {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            Transaction transaction = event.getTransaction();

            // Only care about items being added (pickup), not removed or moved
            if (!(transaction instanceof ItemStackTransaction itemTransaction)) {
                return;
            }

            ActionType action = itemTransaction.getAction();
            if (action == null || !action.isAdd()) {
                return;
            }

            // Get the item that was added
            if (itemTransaction.getQuery() == null) {
                return;
            }
            String itemName = itemTransaction.getQuery().getItem().getId();
            String displayName = ItemUtil.getItemName(itemName);

            boolean relevantItem = displayName.matches(".*\\b(?i)(ore|ingot|gem)\\b.*");
            UUID playerId = player.getUuid();

            if (relevantItem && BudRegistry.playerHasBud(playerId)) {
                LoggerUtil.getLogger()
                        .finer(() -> "[BUD] Inventory Change (ADD): " + player.getDisplayName()
                                + " received " + itemName);
                RecentItemCache.getInstance().add(playerId,
                        new ItemEntry(displayName, getPriority(displayName)));
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in InventoryChangeListener: " + e.getMessage());
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
