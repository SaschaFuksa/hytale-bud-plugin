package com.bud.llmworldmessages.bioms;

import com.bud.llmworldmessages.ILLMWorldInfoMessage;
import com.bud.systems.BudWorldContext;

public class LLMWorldBiomeZone5Message implements ILLMWorldInfoMessage {
    
    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        return "A deep wide ocean, surrounded by mysterious depths and marine life. Islands and vulcanos are scattered around.";
    }
}
