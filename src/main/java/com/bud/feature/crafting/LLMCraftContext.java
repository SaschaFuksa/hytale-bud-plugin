package com.bud.feature.crafting;

import java.util.Map;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.item.ItemMessage;
import com.bud.feature.item.ItemUtil;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMCraftContext(CraftEntry craftEntry) implements IPromptContext {

    @Nonnull
    public static LLMCraftContext from(CraftEntry entry) {
        return new LLMCraftContext(entry);
    }

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

    public String getCraftingInformation() {
        if (this.craftEntry.interaction() == CraftInteraction.USED) {
            return "Your Buddy just used a crafting station: " + getDisplayName();
        }
        return "Your Buddy just crafted: " + getDisplayName() + ".";
    }

    @Override
    public BudComponent getBudComponent() {
        return craftEntry.getBudComponent();
    }

    @Override
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(craftEntry.getBudComponent().getBudType());
    }
}
