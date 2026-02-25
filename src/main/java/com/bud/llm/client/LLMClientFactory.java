package com.bud.llm.client;

import com.bud.core.config.LLMConfig;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMClientFactory {

    public static ILLMClient createClient() {
        LLMConfig config = LLMConfig.getInstance();

        if (config.isUsePlayer2API()) {
            LoggerUtil.getLogger().info(() -> "[LLM] Using Player2 LLM");
            return new Player2LLMClient();
        } else {
            LoggerUtil.getLogger().info(() -> "[LLM] Using legacy LLM");
            return new BudLLMClient();
        }
    }
}
