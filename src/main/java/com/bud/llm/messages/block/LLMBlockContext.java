package com.bud.llm.messages.block;

import com.bud.components.BudComponent;
import com.bud.llm.messages.IPromptContext;
import com.bud.profile.IBudProfile;
import com.bud.reaction.block.BlockInteraction;
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
