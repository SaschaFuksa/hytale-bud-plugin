package com.bud.llm.llmworldmessage;

import com.bud.system.BudWorldContext;

public interface ILLMWorldInfoMessage {

    String getMessageForContext(BudWorldContext context, String additionalInfo);

}
