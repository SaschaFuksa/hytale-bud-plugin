package com.bud.llm.message.weather;

import com.bud.llm.message.creation.ILLMMessageCreation;
import com.bud.llm.message.creation.IPromptContext;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.prompt.BudMessage;
import com.bud.llm.message.prompt.LLMPromptManager;

public class LLMWeatherMessageCreation implements ILLMMessageCreation {

    @Override
    public Prompt createPrompt(IPromptContext context, BudMessage npcMessage) {
        if (!(context instanceof LLMWeatherContext weatherContext)) {
            throw new IllegalArgumentException("Context must be of type LLMWeatherContext");
        }

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String weatherInfo = weatherContext.getWeatherInformation();
        String weatherView = npcMessage.getPersonalWeatherView();

        String systemPrompt = manager.getSystemPrompt("weather") + "\n"
                + manager.getSystemPrompt("default") + "\n"
                + budInfo + "\n" + weatherView;

        String userPrompt = weatherInfo + "\n" + manager.getSystemPrompt("final");

        return new Prompt(systemPrompt, userPrompt);
    }
}
