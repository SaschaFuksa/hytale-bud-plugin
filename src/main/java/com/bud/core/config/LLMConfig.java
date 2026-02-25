package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class LLMConfig {

    public static final BuilderCodec<LLMConfig> CODEC;

    private boolean enableLLM = true;
    private boolean usePlayer2API = false;
    private String url = "http://192.168.178.25:1234/v1/chat/completions";
    private String model = "mistralai/ministral-3-3b"; // like "mistralai/ministral-3-3b", "qwen/qwen3-1.7b"
    private String apiKey = "not_needed";
    private int maxTokens = 100;
    private double temperature = 0.9;

    private static volatile LLMConfig instance;

    public static void setInstance(LLMConfig config) {
        instance = config;
    }

    public static LLMConfig getInstance() {
        LLMConfig config = instance;
        if (config == null) {
            instance = new LLMConfig();
        }
        return instance;
    }

    public boolean isEnableLLM() {
        return this.enableLLM;
    }

    public boolean isUsePlayer2API() {
        return this.usePlayer2API;
    }

    public String getUrl() {
        return this.url;
    }

    public String getModel() {
        return this.model;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public int getMaxTokens() {
        return this.maxTokens;
    }

    public double getTemperature() {
        return this.temperature;
    }

    static {
        CODEC = BuilderCodec.builder(LLMConfig.class, LLMConfig::new)
                .append(new KeyedCodec<>("EnableLLM", Codec.BOOLEAN),
                        (config, value) -> config.enableLLM = value,
                        config -> config.enableLLM)
                .add()
                .append(new KeyedCodec<>("UsePlayer2API", Codec.BOOLEAN),
                        (config, value) -> config.usePlayer2API = value,
                        config -> config.usePlayer2API)
                .add()
                .append(new KeyedCodec<>("Url", Codec.STRING),
                        (config, value) -> config.url = value,
                        config -> config.url)
                .add()
                .append(new KeyedCodec<>("Model", Codec.STRING),
                        (config, value) -> config.model = value,
                        config -> config.model)
                .add()
                .append(new KeyedCodec<>("ApiKey", Codec.STRING),
                        (config, value) -> config.apiKey = value,
                        config -> config.apiKey)
                .add()
                .append(new KeyedCodec<>("MaxTokens", Codec.INTEGER),
                        (config, value) -> config.maxTokens = value,
                        config -> config.maxTokens)
                .add()
                .append(new KeyedCodec<>("Temperature", Codec.DOUBLE),
                        (config, value) -> config.temperature = value,
                        config -> config.temperature)
                .add()
                .build();
    }

}
