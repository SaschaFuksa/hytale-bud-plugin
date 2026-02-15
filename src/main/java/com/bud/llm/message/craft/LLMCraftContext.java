package com.bud.llm.message.craft;

import java.util.Map;

import com.bud.llm.message.IPromptContext;
import com.bud.llm.message.prompt.ItemPromptMessage;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.reaction.ItemUtil;
import com.bud.reaction.crafting.CraftEntry;
import com.bud.reaction.crafting.CraftInteraction;

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
            case "interaction" -> this.craftEntry.interaction().name();
            default -> null;
        };
    }

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
            ItemPromptMessage itemPrompt = LLMPromptManager.getInstance().getItemPromptMessage();
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
}
