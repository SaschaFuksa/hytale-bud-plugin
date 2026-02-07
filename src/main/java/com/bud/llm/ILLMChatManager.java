package com.bud.llm;

import java.util.UUID;

import com.bud.llm.message.creation.Prompt;
import com.bud.npc.BudInstance;
import com.bud.result.IDataResult;

public interface ILLMChatManager {

    IDataResult<Prompt> generatePrompt(BudInstance budInstance);

    BudInstance getBudInstance(UUID ownerId);

    String getFallbackMessage(BudInstance budInstance);

}
