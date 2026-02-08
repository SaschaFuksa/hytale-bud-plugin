package com.bud.llm.message.block;

import com.bud.llm.message.creation.IPromptContext;
import com.bud.reaction.block.BlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public record LLMBlockContext(String blockName, BlockInteraction interaction, PlayerRef player)
        implements IPromptContext {

    @Override
    public String getContextById(String id) {
        if ("blockName".equals(id))
            return blockName;
        if ("player".equals(id))
            return player.getUsername();
        if ("interaction".equals(id))
            return interaction.name();
        return "";
    }

    public static LLMBlockContext from(String blockName, BlockInteraction interaction, PlayerRef player) {
        return new LLMBlockContext(blockName, interaction, player);
    }
}
