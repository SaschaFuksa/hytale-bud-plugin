package com.bud.llm.llmworldmessage;

import com.bud.system.BudWorldContext;

public class LLMWorldZoneMessage implements ILLMWorldInfoMessage {

    @Override
    public String getMessageForContext(BudWorldContext context, String additionalInfo) {
        String currentZoneName = context.currentZone().name().toLowerCase();
        if (currentZoneName.contains("1") || currentZoneName.contains("emerald") || currentZoneName.contains("grove")) {
            return null;
        } else if (currentZoneName.contains("2") || currentZoneName.contains("howling")
                || currentZoneName.contains("sands")) {
            return null;
        } else if (currentZoneName.contains("3") || currentZoneName.contains("whisperfrost")
                || currentZoneName.contains("frontiers")) {
            return null;
        } else if (currentZoneName.contains("4") || currentZoneName.contains("devastated")) {
            return null;
        } else if (currentZoneName.contains("ocean")) {
            return null;
        }
        return "The current zone is unknown.";
    }

}