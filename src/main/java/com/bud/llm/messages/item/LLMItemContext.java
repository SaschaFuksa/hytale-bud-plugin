package com.bud.llm.messages.item;

import com.bud.components.BudComponent;
import com.bud.llm.messages.IPromptContext;
import com.bud.profile.IBudProfile;
import com.bud.reaction.item.ItemEntry;

public record LLMItemContext(ItemEntry itemEntry) implements IPromptContext {

    public static LLMItemContext from(ItemEntry itemEntry) {
        return new LLMItemContext(itemEntry);
    }

    public String getCollectInformation() {
        return "Your Buddy collected following item: " + this.itemEntry.itemName();
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
