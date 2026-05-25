// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package com.bud.llm.client;

import com.bud.core.config.LLMConfig;
import com.bud.llm.prompt.Prompt;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.logging.Logger;

public class BudLLMClient extends AbstractLLMClient {
        private static final Logger LOG = Logger.getLogger("BudPlugin-AuthFix");
        private final HttpClient httpClient;

        public BudLLMClient() {
                this.httpClient = HttpClient.newBuilder().version(Version.HTTP_1_1)
                                .connectTimeout(Duration.ofSeconds(10L)).build();
        }

        public String callLLM(Prompt var1) throws IOException, InterruptedException {
                LLMConfig var2 = LLMConfig.getInstance();
                String var3 = JsonUtils.escapeJson(var1.systemPrompt());
                String var4 = JsonUtils.escapeJson(var1.userPrompt());
                String var10000 = var2.getModel();
                String var5 = "{\"model\":\"" + var10000 + "\",\"messages\":[{\"role\":\"system\",\"content\":\"" + var3
                                + "\"},{\"role\":\"user\",\"content\":\"" + var4 + "\"}],\"temperature\":"
                                + var2.getTemperature() + ",\"max_tokens\":" + var2.getMaxTokens() + "}";
                String var6 = var2.getUrl();
                HttpRequest.Builder var7 = HttpRequest.newBuilder().uri(URI.create(var6)).header("Content-Type",
                                "application/json");
                String var8 = var2.getApiKey();
                boolean var9 = var6 != null && var6.toLowerCase(Locale.ROOT).contains("openrouter.ai");
                boolean var10 = var8 != null && !var8.isBlank() && !"not_needed".equalsIgnoreCase(var8)
                                && !var8.contains("PUT_YOUR") && !var8.contains("REDACTED");
                if (var10) {
                        var7.header("Authorization", "Bearer " + var8.trim());
                } else if (var9) {
                        LOG.severe("[LLM] OpenRouter URL configured but ApiKey is missing or placeholder.");
                        throw new IOException("OpenRouter ApiKey missing or placeholder");
                }

                HttpRequest var11 = var7.POST(BodyPublishers.ofString(var5)).timeout(Duration.ofSeconds(10L)).build();
                LOG.info("[LLM] Sending request to " + var6);
                HttpResponse var12 = this.httpClient.send(var11, BodyHandlers.ofString(StandardCharsets.UTF_8));
                LOG.info("[LLM] Response code: " + var12.statusCode());
                if (var12.statusCode() != 200) {
                        int var10002 = var12.statusCode();
                        throw new IOException("API Error: " + var10002 + " " + (String) var12.body());
                } else {
                        String var13 = (String) var12.body();
                        this.logUsage("LLM", var13);
                        return this.extractContent(var13);
                }
        }
}
