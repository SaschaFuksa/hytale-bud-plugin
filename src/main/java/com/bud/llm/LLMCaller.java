package com.bud.llm;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import com.bud.llm.client.ILLMClient;
import com.bud.llm.client.LLMClientFactory;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMCaller {

    private static final LLMCaller INSTANCE = new LLMCaller();

    private LLMCaller() {
        this.llmClient = LLMClientFactory.createClient();
    }

    private final ILLMClient llmClient;

    public static LLMCaller getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<String> callLLM(Prompt prompt, IBudProfile budProfile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = this.llmClient.callLLM(prompt);
                String message = budProfile.getNPCDisplayName() + ": " + response;
                LoggerUtil.getLogger().info(() -> "[BUD] LLM response: " + message);
                return message;
            } catch (IOException | InterruptedException e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] LLM Error: " + e.getMessage());
                return null;
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

}
