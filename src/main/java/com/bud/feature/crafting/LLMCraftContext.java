package com.bud.feature.crafting;

import java.util.Map;

import com.bud.core.components.BudComponent;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.feature.item.ItemMessage;
import com.bud.feature.item.ItemUtil;
import com.bud.feature.profile.IBudProfile;

/**
 * Context for crafting LLM prompts.
 * Wraps a CraftEntry and provides formatted item information.
 */
public record LLMCraftContext(CraftEntry craftEntry) implements IPromptContext {

    public static LLMCraftContext from(CraftEntry entry) {
        return new LLMCraftContext(entry);
    }

    /**
     * Gets the formatted display name.
     * For CRAFTED: converts item IDs like "Tool_Hatchet_Crude" to "Tool Hatchet
     * Crude".
     * For USED: looks up bench description from items.yml, falls back to display
     * name.
     */
    public String getDisplayName() {
        if (this.craftEntry.interaction() == CraftInteraction.USED) {
            ItemMessage itemPrompt = LLMPromptManager.getInstance().getItemPromptMessage();
            Map<String, String> bench = itemPrompt.getBench();
            if (bench != null && bench.containsKey(this.craftEntry.itemId())) {
                return bench.get(this.craftEntry.itemId());
            }
        }
        return ItemUtil.getDisplayName(this.craftEntry.itemId());
    }

    /**
     * Creates the crafting notification text for the user prompt.
     */
    public String getCraftingInformation() {
        if (this.craftEntry.interaction() == CraftInteraction.USED) {
            return "Your Buddy just used a crafting station: " + getDisplayName();
        }
        return "Your Buddy just crafted: " + getDisplayName() + ".";
    }

    @Override
    public BudComponent getBudComponent() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudComponent'");
    }

    @Override
    public IBudProfile getBudProfile() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBudProfile'");
    }
}
