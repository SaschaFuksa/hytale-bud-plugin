package com.bud.llm.llmclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
                escapeJson(this.systemPrompt),
                escapeJson(message));

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
     * Extract content from Player2 API response
     */
    private String extractContent(String jsonResponse) throws IOException {
        JsonNode rootNode = MAPPER.readTree(jsonResponse);

        // Try OpenAI-style format first (choices[0].message.content)
        if (rootNode.has("choices") && rootNode.get("choices").isArray() && rootNode.get("choices").size() > 0) {
            JsonNode firstChoice = rootNode.get("choices").get(0);
            if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                return firstChoice.get("message").get("content").asText();
            }
        }

        // Try direct content field
        if (rootNode.has("content")) {
            return rootNode.get("content").asText();
        }

        // Try message field
        if (rootNode.has("message")) {
            return rootNode.get("message").asText();
        }

        throw new IOException("Could not extract content from Player2 response: " + jsonResponse);
    }

    /**
     * Escape string for JSON
     */
    private String escapeJson(String input) {
        if (input == null)
            return "\"\"";

        String escaped = input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        return "\"" + escaped + "\"";
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
