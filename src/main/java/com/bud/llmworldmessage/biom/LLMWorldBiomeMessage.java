package com.bud.llmworldmessage.biom;

import com.bud.llmworldmessage.ILLMWorldInfoMessage;
import com.bud.llmworldmessage.LLMWorldZoneMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldBiomeMessage implements ILLMWorldInfoMessage {

    private static final LLMWorldZoneMessage zoneMessage = new LLMWorldZoneMessage();

    // Use specific message handlers for each zone
    private static final ILLMWorldInfoMessage zone1 = new LLMWorldBiomeZone1Message();
    private static final ILLMWorldInfoMessage zone2 = new LLMWorldBiomeZone2Message();
    private static final ILLMWorldInfoMessage zone3 = new LLMWorldBiomeZone3Message();
    private static final ILLMWorldInfoMessage zone4 = new LLMWorldBiomeZone4Message();
    private static final ILLMWorldInfoMessage zone5 = new LLMWorldBiomeZone5Message();
    
    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String currentBiomeName = context.currentBiome().getName().toLowerCase();
        if (additionalInfo == null) {
            additionalInfo = zoneMessage.getMessageForContext(context, "").toLowerCase();
        }
        
        if (additionalInfo.contains("emerald grove")) {
            return zone1.getMessageForContext(context, additionalInfo);
        } else if (additionalInfo.contains("howling sands")) {
            return zone2.getMessageForContext(context, additionalInfo);
        } else if (additionalInfo.contains("whisperfrost frontiers")) {
            return zone3.getMessageForContext(context, additionalInfo);
        } else if (additionalInfo.contains("devastated lands")) {
            return zone4.getMessageForContext(context, additionalInfo);
        } else if (additionalInfo.contains("ocean")) {
            return zone5.getMessageForContext(context, additionalInfo);
        }
                
        return "The current biome is " + currentBiomeName;
    }

}