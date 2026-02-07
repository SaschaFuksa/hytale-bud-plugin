package com.bud.llm.client;

import com.bud.BudConfig;
import com.bud.util.JsonUtils;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Player2 API LLM client implementation.
 * Simple, stateless LLM calls to Player2 API.
 */
public class Player2LLMClient extends AbstractLLMClient {
    private static final String BASE_URL = "http://localhost:4315";
    private static final String GAME_KEY = "hytale-bud";

    private final HttpClient httpClient;
    private final String systemPrompt;

    public Player2LLMClient(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String callLLM(String message) throws IOException, InterruptedException {
        BudConfig config = getConfig();
        // Build simple JSON payload
        String jsonPayload = String.format(
                "{\"messages\":[{\"role\":\"system\",\"content\":%s},{\"role\":\"user\",\"content\":%s}],\"temperature\":"
                        + config.getTemperature() + ",\"max_tokens\":" + config.getMaxTokens() + "}",
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
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("Player2 API Error: " + response.statusCode() + " " + response.body());
        }

        String responseBody = response.body();
        logUsage("Player2", responseBody);
        return extractContent(responseBody);
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

    private BudConfig getConfig() {
        return BudConfig.getInstance();
    }
}
