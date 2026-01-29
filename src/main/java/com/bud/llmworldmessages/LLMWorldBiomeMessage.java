package com.bud.llmworldmessages;

import com.bud.systems.BudWorldContext;

public class LLMWorldBiomeMessage implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context) {
        String currentBiomeName = context.currentBiome().getName().toLowerCase();
        return "The current zone is unknown.";
    }
    
}