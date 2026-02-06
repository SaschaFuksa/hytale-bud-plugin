package com.bud.llm;

import java.util.UUID;

import com.bud.npc.BudInstance;
import com.bud.result.IDataResult;

public interface ILLMChatContext {

    IDataResult<String> generatePrompt(BudInstance budInstance);

    BudInstance getRandomInstanceForOwner(UUID ownerId);

    String getFallbackMessage(BudInstance budInstance);

}
