package com.bud.llm.client;

import com.bud.BudConfig;
import com.bud.llm.message.prompt.LLMPromptManager;
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
        return createClient(LLMPromptManager.getInstance().getSystemPrompt("default"));
    }

    /**
     * Create an LLM client with custom system prompt.
     * 
     * @param systemPrompt Custom system prompt
     * @return ILLMClient instance (BudLLM or Player2LLM)
     */
    public static ILLMClient createClient(String systemPrompt) {
        BudConfig config = BudConfig.getInstance();

        if (config.isUsePlayer2API()) {
            LoggerUtil.getLogger().info(() -> "[LLM] Using Player2 LLM");
            return new Player2LLMClient(systemPrompt);
        } else {
            // Use legacy LLM
            LoggerUtil.getLogger().info(() -> "[LLM] Using legacy LLM");
            return new BudLLMClient(systemPrompt);
        }
    }
}
