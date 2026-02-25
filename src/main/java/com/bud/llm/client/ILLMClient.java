package com.bud.llm.client;

import java.io.IOException;
import java.util.function.Consumer;

import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public interface ILLMClient {

    String callLLM(Prompt prompt) throws IOException, InterruptedException;

    default void callLLMAsync(Prompt prompt, Consumer<String> onSuccess, Consumer<String> onError) {
        Thread.ofVirtual().start(() -> {
            try {
                String response = callLLM(prompt);
                onSuccess.accept(response);
            } catch (java.io.IOException | InterruptedException e) {
                LoggerUtil.getLogger().severe(() -> "[LLM] Error: " + e.getMessage());
                onError.accept(e.getMessage());
            }
        });
    }

}
