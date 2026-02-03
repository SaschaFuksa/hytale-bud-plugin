package com.bud.llm.llmclient;

import java.io.IOException;
import java.util.function.Consumer;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Interface for LLM clients.
 * Allows switching between different LLM implementations (legacy, Player2,
 * etc.)
 */
public interface ILLMClient {

    /**
     * Call the LLM with a message
     * 
     * @param message User message to send to LLM
     * @return LLM response text
     * @throws IOException          if API call fails
     * @throws InterruptedException if request is interrupted
     */
    String callLLM(String message) throws IOException, InterruptedException;

    /**
     * Call the LLM asynchronously with error handling.
     * Executes in a Virtual Thread and handles exceptions automatically.
     * 
     * @param prompt    The prompt to send to the LLM
     * @param onSuccess Callback with the LLM response (called on success)
     * @param onError   Callback with error message (called on failure)
     */
    default void callLLMAsync(String prompt, Consumer<String> onSuccess, Consumer<String> onError) {
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
