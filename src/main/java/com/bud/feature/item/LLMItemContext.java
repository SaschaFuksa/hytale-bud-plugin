package com.bud.feature.item;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMItemContext(ItemEntry itemEntry) implements IPromptContext {

    @Nonnull
    public static LLMItemContext from(ItemEntry itemEntry) {
        return new LLMItemContext(itemEntry);
    }

    public String getCollectInformation() {
        return "Your Buddy collected following item: " + this.itemEntry.itemName();
    }

    @Override
    public BudComponent getBudComponent() {
        return itemEntry.getBudComponent();
    }

    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(getBudComponent().getBudType());
    }

}
