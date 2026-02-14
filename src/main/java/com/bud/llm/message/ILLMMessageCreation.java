package com.bud.llm.message;

import com.bud.npc.BudInstance;

public interface ILLMMessageCreation {

    Prompt createPrompt(IPromptContext context, BudInstance budInstance);

}
