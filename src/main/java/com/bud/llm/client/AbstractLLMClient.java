package com.bud.llm.client;

import java.io.IOException;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public abstract class AbstractLLMClient implements ILLMClient {

    protected void logUsage(String clientName, String jsonResponse) {
        try {
            Integer totalTokens = JsonUtils.extractInt(jsonResponse, "total_tokens");
            if (totalTokens != null) {
                LoggerUtil.getLogger()
                        .info(() -> "[" + clientName + "] Token Usage: " + totalTokens + " total tokens.");
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().fine(() -> "[" + clientName + "] Could not parse token usage: " + e.getMessage());
        }
    }

    protected String extractContent(String jsonResponse) throws IOException {
        String content = JsonUtils.extractString(jsonResponse, "content");
        if (content == null) {
            throw new IOException("Could not find content field in response");
        }

        return content
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "") // Remove Emojis
                .replace("\u2013", "-") // En dash to normal dash
                .replace("\u2014", "-") // Em dash to normal dash
                .replace("\u2018", "'") // Smart quotes
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .trim();
    }
}
