package com.bud.llm.llmworldmessage;

import com.bud.llm.llmmessage.ILLMBudNPCMessage;
import com.bud.llm.llmworldmessage.biom.LLMWorldBiomeMessage;
import com.bud.system.BudWorldContext;

public class LLMWorldInfoMessageManager {

    public static String createPrompt(BudWorldContext context, ILLMBudNPCMessage npcMessage) {
        String zoneInfo = new LLMWorldZoneMessage().getMessageForContext(context, "");
        String biomeInfo = new LLMWorldBiomeMessage().getMessageForContext(context, zoneInfo);
        String timeInfo = new LLMWorldTimeMessage().getMessageForContext(context, "");
        String budInfo = npcMessage.getSystemPrompt();
        String introduction = """
                You want to say something about the current environment you are in.
                The following text provides detailed information about your environment.
                After the environment information, you get information about yourself: what you like or dislike.
                Don't mention the environment zone or biome too often.
                Finally, say something short and related to your current environment and personal view.
                """;
        String environment_info = """
                The current environment details are as follows:
                You are in the following zone: %s
                In this zone, the current biome is described as: %s
                And the current time of day is: %s
                """.formatted(zoneInfo, biomeInfo, timeInfo);
        String bud_info = npcMessage.getPersonalWorldView();
        return budInfo + "\n" + introduction + "\n" + environment_info + "\n" + bud_info;

    }

}
