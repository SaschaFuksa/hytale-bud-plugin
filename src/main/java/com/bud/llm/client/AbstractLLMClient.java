package com.bud.llm.client;

import java.io.IOException;
import java.util.regex.Pattern;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public abstract class AbstractLLMClient implements ILLMClient {

    private static final Pattern THINK_TAG_PATTERN = Pattern.compile("(?s)<think>.*?</think>");
    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");

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

        String withoutThinkTags = THINK_TAG_PATTERN.matcher(content).replaceAll("");
        String withoutEmoji = EMOJI_PATTERN.matcher(withoutThinkTags).replaceAll("");
        return withoutEmoji
                .replace("\u2013", "-")
                .replace("\u2014", "-")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .trim();
    }
}
