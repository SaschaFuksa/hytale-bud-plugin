package com.bud.llm.message.item;

import com.bud.llm.message.IPromptContext;

public record LLMItemContext(String itemName) implements IPromptContext {

    @Override
    public String getContextById(String contextId) {
        // Implement this method based on your context retrieval logic
        return switch (contextId) {
            case "itemName" -> this.itemName;
            default -> null;
        };
    }

    public static LLMItemContext from(String itemName) {
        return new LLMItemContext(itemName);
    }

    public static String getItemInformation(String itemName) {
        return "Your Buddy collected following item: " + itemName;
    }

}
