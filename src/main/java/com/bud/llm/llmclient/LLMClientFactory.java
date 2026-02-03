package com.bud.llm.llmclient;

import com.bud.BudConfig;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Factory for creating LLM client instances.
 * Decides between BudLLM (legacy) and Player2LLM based on config.
 */
public class LLMClientFactory {

    /**
     * Create an LLM client with default system prompt.
     * 
     * @return ILLMClient instance (BudLLM or Player2LLM)
     */
    public static ILLMClient createClient() {
        return createClient(ILLMClient.DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * Create an LLM client with custom system prompt.
     * 
     * @param systemPrompt Custom system prompt
     * @return ILLMClient instance (BudLLM or Player2LLM)
     */
    public static ILLMClient createClient(String systemPrompt) {
        BudConfig config = BudConfig.get();

        if (config.isUsePlayer2API()) {
            LoggerUtil.getLogger().info(() -> "[LLM] Using Player2 LLM");
            return new Player2LLM(systemPrompt);
        } else {
            // Use legacy LLM
            LoggerUtil.getLogger().info(() -> "[LLM] Using legacy LLM");
            return new BudLLM(systemPrompt);
        }
    }
}
