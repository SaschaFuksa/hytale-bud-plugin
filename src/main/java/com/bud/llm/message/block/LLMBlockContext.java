package com.bud.llm.message.block;

import com.bud.llm.message.creation.IPromptContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public record LLMBlockContext(String blockName, PlayerRef player) implements IPromptContext {
    @Override
    public String getContextById(String id) {
        if ("blockName".equals(id))
            return blockName;
        if ("player".equals(id))
            return player.getUsername();
        return "";
    }
}
