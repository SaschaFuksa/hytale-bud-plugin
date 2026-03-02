package com.bud.feature.crafting;

import java.util.Map;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.item.ItemMessage;
import com.bud.feature.item.ItemUtil;
import com.bud.feature.queue.IQueueEntry;

public record CraftEntry(@Nonnull String itemId, @Nonnull CraftInteraction interaction,
        @Nonnull BudComponent budComponent) implements IQueueEntry {

    public String getDisplayName() {
        if (this.interaction == CraftInteraction.USED) {
            ItemMessage itemPrompt = LLMPromptManager.getInstance().getItemPromptMessage();
            Map<String, String> bench = itemPrompt.getBench();
            if (bench != null && bench.containsKey(this.itemId)) {
                return bench.get(this.itemId);
            }
        }
        return ItemUtil.getDisplayName(this.itemId);
    }

    public String getCraftingInformation() {
        if (this.interaction == CraftInteraction.USED) {
            return "Your Buddy just used a crafting station: " + getDisplayName();
        }
        return "Your Buddy just crafted: " + getDisplayName() + ".";
    }

    @Override
    public int getPriority() {
        return (interaction == CraftInteraction.CRAFTED) ? 1 : 2;
    }

    @Nonnull
    @Override
    public BudComponent getBudComponent() {
        return budComponent;
    }

    @Nonnull
    @Override
    public String getEntryName() {
        return itemId;
    }
}
