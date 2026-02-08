package com.bud.llm.message.creation;

import com.bud.llm.message.prompt.BudMessage;

public interface ILLMMessageCreation {

    Prompt createPrompt(IPromptContext context, BudMessage npcMessage);

}
