package com.bud.llm;

import com.bud.npc.BudInstance;
import com.bud.result.IDataResult;

public interface ILLMChatContext {

    IDataResult<String> generatePrompt(BudInstance budInstance);

}
