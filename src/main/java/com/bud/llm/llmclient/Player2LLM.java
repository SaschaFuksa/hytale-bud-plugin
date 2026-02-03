package com.bud.llm.llmclient;

import com.bud.util.JsonUtils;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Player2 API LLM client implementation.
 * Simple, stateless LLM calls to Player2 API.
 */
public class Player2LLM implements ILLMClient {
    private static final String BASE_URL = "http://localhost:4315";
    private static final String GAME_KEY = "hytale-bud";

    private final HttpClient httpClient;
    private final String systemPrompt;

    public Player2LLM(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String callLLM(String message) throws IOException, InterruptedException {
        // Build simple JSON payload
        String jsonPayload = String.format(
                "{\"messages\":[{\"role\":\"system\",\"content\":%s},{\"role\":\"user\",\"content\":%s}],\"temperature\":0.8,\"max_tokens\":400}",
                JsonUtils.escapeJsonWithQuotes(this.systemPrompt),
                JsonUtils.escapeJsonWithQuotes(message));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/v1/chat/completions"))
                .header("player2-game-key", GAME_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(30))
                .build();

        LoggerUtil.getLogger().fine(() -> "[Player2] Sending request...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Player2 API Error: " + response.statusCode() + " " + response.body());
        }

        return extractContent(response.body());
    }

    /**
     * Extract content from Player2 API response using manual JSON parsing
     */
    private String extractContent(String jsonResponse) throws IOException {
        // Try OpenAI-style format: choices[0].message.content
        int contentIdx = jsonResponse.indexOf("\"content\":");
        if (contentIdx == -1) {
            throw new IOException("Could not find content field in response: " + jsonResponse);
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

        return jsonResponse.substring(openQuoteIdx + 1, closeQuoteIdx)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * Test if Player2 is available
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/v1/health"))
                    .header("player2-game-key", GAME_KEY)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            return false;
        }
    }
}
