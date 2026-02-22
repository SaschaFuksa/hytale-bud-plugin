package com.bud.llm;

import java.util.Set;
import java.util.UUID;

import com.bud.llm.messages.Prompt;
import com.bud.npc.BudInstance;
import com.bud.result.IDataResult;

public interface ILLMChatManager {

    IDataResult<Prompt> generatePrompt(BudInstance budInstance);

    Set<BudInstance> getRelevantBudInstances(UUID ownerId);

    String getFallbackMessage(BudInstance budInstance);

}
