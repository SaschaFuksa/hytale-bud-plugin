package com.bud.llm.messages.item;

import com.bud.llm.messages.IPromptContext;
import com.bud.reaction.item.ItemEntry;

public record LLMItemContext(ItemEntry itemEntry) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        // Implement this method based on your context retrieval logic
        return switch (contextId) {
            case "itemName" -> this.itemEntry.itemName();
            default -> null;
        };
    }

    public static LLMItemContext from(ItemEntry itemEntry) {
        return new LLMItemContext(itemEntry);
    }

    public String getCollectInformation() {
        return "Your Buddy collected following item: " + this.itemEntry.itemName();
    }

}
