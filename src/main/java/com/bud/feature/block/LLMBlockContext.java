package com.bud.feature.block;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;

public record LLMBlockContext(@Nonnull BlockEntry blockEntry)
        implements IPromptContext {

    @Nonnull
    public static LLMBlockContext from(@Nonnull BlockEntry blockEntry) {
        return new LLMBlockContext(blockEntry);
    }

    @Override
    @Nonnull
    public BudComponent getBudComponent() {
        return blockEntry.getBudComponent();
    }

    @Override
    @Nonnull
    public IBudProfile getBudProfile() {
        return BudProfileMapper.getInstance().getProfileForBudType(blockEntry.getBudComponent().getBudType());
    }

}
