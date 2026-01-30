package com.bud.llmworldmessage;

import com.bud.llmmessage.ILLMBudNPCMessage;
import com.bud.llmworldmessage.biom.LLMWorldBiomeMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldInfoMessageManager {

    public static String createPrompt(BudWorldContext context, ILLMBudNPCMessage npcMessage) {
        String zoneInfo = new LLMWorldZoneMessage().getMessageForContext(context, "");
        String biomeInfo = new LLMWorldBiomeMessage().getMessageForContext(context, zoneInfo);
        String timeInfo = new LLMWorldTimeMessage().getMessageForContext(context, "");
        String introduction = """
                You want to say something about the current environment you are in.
                Next you get detailed information about your environment.
                After you got information about yourself, what you like or disklike.
                Don't mention the zone or biome too often.
                Finally, say something short and related to your current environment and personal world view.
                """;
        String environment_info = """
                The current environment details are as follows:
                You are in following zone: %s
                In this zone, the current biome is described as: %s
                And the current time of day is: %s
                """.formatted(zoneInfo, biomeInfo, timeInfo);
        String bud_info = npcMessage.getPersonalWorldView();
        return introduction + "\n" + environment_info + "\n" + bud_info;

    }
    
}
