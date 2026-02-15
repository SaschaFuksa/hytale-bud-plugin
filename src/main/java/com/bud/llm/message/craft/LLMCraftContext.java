package com.bud.llm.message.craft;

import com.bud.llm.message.IPromptContext;
import com.bud.reaction.ItemUtil;
import com.bud.reaction.crafting.CraftEntry;

/**
 * Context for crafting LLM prompts.
 * Wraps a CraftEntry and provides formatted item information.
 */
public record LLMCraftContext(CraftEntry craftEntry) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        return switch (contextId) {
            case "itemId" -> this.craftEntry.itemId();
            case "displayName" -> getDisplayName();
            default -> null;
        };
    }

    public static LLMCraftContext from(CraftEntry entry) {
        return new LLMCraftContext(entry);
    }

    /**
     * Gets the formatted display name of the crafted item.
     * Converts IDs like "Tool_Hatchet_Crude" to "Tool Hatchet Crude".
     */
    public String getDisplayName() {
        return ItemUtil.getDisplayName(this.craftEntry.itemId());
    }

    /**
     * Creates the crafting notification text for the user prompt.
     */
    public String getCraftingInformation() {
        return "Your Buddy just crafted: " + getDisplayName() + ".";
    }
}
