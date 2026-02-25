package com.bud.feature.item;

import com.bud.core.components.BudComponent;
import com.bud.llm.prompt.IPromptContext;
import com.bud.feature.profile.IBudProfile;

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
