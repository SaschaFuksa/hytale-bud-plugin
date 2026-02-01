package com.bud.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.bud.BudConfig;

public class BudLLM {
    private final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    private final String systemPrompt;
    private final BudConfig budConfig = BudConfig.get();

    public BudLLM() {
        this.systemPrompt = "Answer short, Keep responses concise and entertaining. Don't ask for follow up questions. Only response with maximum 1 sentences.";
    }

    public BudLLM(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public boolean isEnabled() {
        return this.budConfig.isEnableLLM();
    }

    public String callLLM(String message) throws IOException, InterruptedException {
        String escapedSystemPrompt = escapeJson(this.systemPrompt);
        String escapedMessage = escapeJson(message);
        String jsonPayload = "{\"model\":\"" + this.budConfig.getModel() + "\",\"messages\":[{\"role\":\"system\",\"content\":\"" + escapedSystemPrompt + "\"},{\"role\":\"user\",\"content\":\"" + escapedMessage + "\"}],\"temperature\":0.8,\"max_tokens\":400}";

        System.out.println("[LLM] Sending request to " + budConfig.getUrl());
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(budConfig.getUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(10))
                .build();

        System.out.println("[LLM] Waiting for response...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("[LLM] Response code: " + response.statusCode());
        
        if (response.statusCode() != 200) {
            throw new IOException("API Error: " + response.statusCode() + " " + response.body());
        }

        return getContentFromResponse(response.body());
    }

    private String getContentFromResponse(String jsonResponse) {
        // Better JSON parsing - find the content field and extract it properly
        System.out.println("[LLM] Full response: " + jsonResponse);
        
        try {
            // Find "content": in the response
            int contentIdx = jsonResponse.indexOf("\"content\":");
            if (contentIdx == -1) {
                return "Could not find content field in response";
            }
            
            // Skip past "content": to find the opening quote
            int openQuoteIdx = jsonResponse.indexOf("\"", contentIdx + 10);
            if (openQuoteIdx == -1) {
                return "Could not find opening quote after content field";
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
                return "Could not find closing quote for content";
            }
            
            String content = jsonResponse.substring(openQuoteIdx + 1, closeQuoteIdx)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            
            System.out.println("[LLM] Extracted content: " + content);
            return content;
            
        } catch (Exception e) {
            System.out.println("[LLM] Parsing error: " + e.getMessage());
            return "Error parsing response: " + e.getMessage();
        }
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

}
