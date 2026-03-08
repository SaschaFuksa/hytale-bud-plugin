package com.bud.llm;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bud.llm.client.ILLMClient;
import com.bud.llm.client.LLMClientFactory;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class LLMCaller {

    private static final LLMCaller INSTANCE = new LLMCaller();
    private static final Pattern TRAILING_FORMATTED_ASIDE = Pattern
            .compile("(?is)\\s+[\\*_`~]+\\s*[\\(\\[].*$");
    private static final Pattern TRAILING_LABELED_META = Pattern
            .compile("(?is)\\s+[\\(\\[]?\\s*(focus|thought|reasoning|note|internal|aside|commentary|meta|ooc)\\s*:");

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
                String message = formatBudResponse(budProfile.getNPCDisplayName(), response);
                LoggerUtil.getLogger().info(() -> "[BUD] LLM response: " + message);
                return message;
            } catch (IOException | InterruptedException e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] LLM Error: " + e.getMessage());
                return null;
            }
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private String formatBudResponse(String budName, String response) {
        String sanitizedResponse = sanitizeResponse(response);
        String prefix = budName + ":";
        if (sanitizedResponse.regionMatches(true, 0, prefix, 0, prefix.length())) {
            sanitizedResponse = sanitizedResponse.substring(prefix.length()).trim();
        }
        if (sanitizedResponse.isBlank()) {
            return "";
        }
        return prefix + " " + sanitizedResponse;
    }

    private String sanitizeResponse(String response) {
        String sanitized = response == null ? "" : response.trim();
        int cutoffIndex = findCutoffIndex(sanitized);
        if (cutoffIndex >= 0) {
            sanitized = sanitized.substring(0, cutoffIndex).trim();
        }
        return sanitized.replaceAll("\\s+", " ").trim();
    }

    private int findCutoffIndex(String response) {
        int cutoffIndex = -1;

        Matcher formattedAsideMatcher = TRAILING_FORMATTED_ASIDE.matcher(response);
        if (formattedAsideMatcher.find()) {
            cutoffIndex = formattedAsideMatcher.start();
        }

        Matcher labeledMetaMatcher = TRAILING_LABELED_META.matcher(response);
        if (labeledMetaMatcher.find()) {
            cutoffIndex = cutoffIndex < 0 ? labeledMetaMatcher.start()
                    : Math.min(cutoffIndex, labeledMetaMatcher.start());
        }

        return cutoffIndex;
    }

}
