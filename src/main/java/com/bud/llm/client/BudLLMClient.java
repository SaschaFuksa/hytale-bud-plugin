package com.bud.llm.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.bud.BudConfig;
import com.bud.util.JsonUtils;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Legacy LLM client implementation.
 * Calls your own LLM server directly.
 */
public class BudLLMClient extends AbstractLLMClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String systemPrompt;

    public BudLLMClient(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String callLLM(String message) throws IOException, InterruptedException {
        BudConfig config = getConfig();
        String escapedSystemPrompt = JsonUtils.escapeJson(this.systemPrompt);
        LoggerUtil.getLogger().info(() -> "[LLM] Systemprompt: " + escapedSystemPrompt);
        String escapedMessage = JsonUtils.escapeJson(message);
        LoggerUtil.getLogger().info(() -> "[LLM] Message: " + escapedMessage);
        String jsonPayload = "{\"model\":\"" + config.getModel()
                + "\",\"messages\":[{\"role\":\"system\",\"content\":\"" + escapedSystemPrompt
                + "\"},{\"role\":\"user\",\"content\":\"" + escapedMessage
                + "\"}],\"temperature\":" + config.getTemperature() + ",\"max_tokens\":" + config.getMaxTokens()
                + "}";

        LoggerUtil.getLogger().info(() -> "[LLM] Sending request to " + config.getUrl());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(10))
                .build();

        LoggerUtil.getLogger().info(() -> "[LLM] Waiting for response...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        LoggerUtil.getLogger().info(() -> "[LLM] Response code: " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new IOException("API Error: " + response.statusCode() + " " + response.body());
        }

        String responseBody = response.body();
        logUsage("LLM", responseBody);
        return extractContent(responseBody);
    }

    private BudConfig getConfig() {
        return BudConfig.getInstance();
    }
}
