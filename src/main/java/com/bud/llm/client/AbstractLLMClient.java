package com.bud.llm.client;

import java.io.IOException;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Base class for LLM clients providing shared utility methods for logging and
 * parsing.
 */
public abstract class AbstractLLMClient implements ILLMClient {

    protected void logUsage(String clientName, String jsonResponse) {
        try {
            int usageIdx = jsonResponse.indexOf("\"usage\":");
            if (usageIdx != -1) {
                int totalTokensIdx = jsonResponse.indexOf("\"total_tokens\":", usageIdx);
                if (totalTokensIdx != -1) {
                    int colonIdx = jsonResponse.indexOf(":", totalTokensIdx);
                    int commaIdx = jsonResponse.indexOf(",", colonIdx);
                    int braceIdx = jsonResponse.indexOf("}", colonIdx);
                    int endIdx = (commaIdx != -1 && (braceIdx == -1 || commaIdx < braceIdx)) ? commaIdx : braceIdx;

                    if (endIdx != -1) {
                        String tokens = jsonResponse.substring(colonIdx + 1, endIdx).trim();
                        LoggerUtil.getLogger()
                                .info(() -> "[" + clientName + "] Token Usage: " + tokens + " total tokens.");
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().fine(() -> "[" + clientName + "] Could not parse token usage: " + e.getMessage());
        }
    }

    protected String extractContent(String jsonResponse) throws IOException {
        int contentIdx = jsonResponse.indexOf("\"content\":");
        if (contentIdx == -1) {
            throw new IOException("Could not find content field in response");
        }

        // Skip past "content": to find the opening quote
        int openQuoteIdx = jsonResponse.indexOf("\"", contentIdx + 10);
        if (openQuoteIdx == -1) {
            throw new IOException("Could not find opening quote after content field");
        }

        // Find the closing quote (handling escaped quotes)
        int closeQuoteIdx = openQuoteIdx + 1;
        while (closeQuoteIdx < jsonResponse.length()) {
            if (jsonResponse.charAt(closeQuoteIdx) == '"' &&
                    jsonResponse.charAt(closeQuoteIdx - 1) != '\\') {
                break;
            }
            closeQuoteIdx++;
        }

        if (closeQuoteIdx >= jsonResponse.length()) {
            throw new IOException("Could not find closing quote for content");
        }

        final int finalOpenQuoteIdx = openQuoteIdx;
        final int finalCloseQuoteIdx = closeQuoteIdx;
        LoggerUtil.getLogger()
                .fine(() -> "Extracted content: " + jsonResponse.substring(finalOpenQuoteIdx + 1, finalCloseQuoteIdx));

        return jsonResponse.substring(openQuoteIdx + 1, closeQuoteIdx)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
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
