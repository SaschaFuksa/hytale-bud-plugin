package com.bud.feature.block;

import com.bud.core.components.BudComponent;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public record LLMBlockContext(String blockName, BlockInteraction interaction, PlayerRef player)
        implements IPromptContext {

    public static LLMBlockContext from(String blockName, BlockInteraction interaction, PlayerRef player) {
        return new LLMBlockContext(blockName, interaction, player);
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
