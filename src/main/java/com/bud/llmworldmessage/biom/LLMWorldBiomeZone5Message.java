package com.bud.llmworldmessage.biom;

import com.bud.llmworldmessage.ILLMWorldInfoMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldBiomeZone5Message implements ILLMWorldInfoMessage {
    
    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        return "A deep wide ocean, surrounded by mysterious depths and marine life. Islands and vulcanos are scattered around.";
    }
}
