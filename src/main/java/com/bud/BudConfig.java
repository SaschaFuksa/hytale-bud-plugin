package com.bud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class BudConfig {
    public static final BuilderCodec<BudConfig> CODEC;

    private boolean enableLLM = true;
    private boolean usePlayer2API = false;
    private String url = "http://192.168.178.25:1234/v1/chat/completions";
    private String model = "mistralai/ministral-3-3b"; // like "mistralai/ministral-3-3b", "qwen/qwen3-1.7b"
    private String apiKey = "not_needed";
    private int maxTokens = 200;
    private double temperature = 0.8;
    private boolean enableCombatReactions = true;
    private boolean enableWorldReactions = true;
    private boolean enableBlockReactions = true;

    private static volatile BudConfig instance;

    public static void setInstance(BudConfig config) {
        instance = config;
    }

    public static BudConfig getInstance() {
        BudConfig config = instance;
        if (config == null) {
            instance = new BudConfig();
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

    public boolean isEnableCombatReactions() {
        return this.enableCombatReactions;
    }

    public boolean isEnableWorldReactions() {
        return this.enableWorldReactions;
    }

    public boolean isEnableBlockReactions() {
        return this.enableBlockReactions;
    }

    static {
        CODEC = BuilderCodec.builder(BudConfig.class, BudConfig::new)
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
                .append(new KeyedCodec<>("EnableCombatReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableCombatReactions = value,
                        config -> config.enableCombatReactions)
                .add()
                .append(new KeyedCodec<>("EnableWorldReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableWorldReactions = value,
                        config -> config.enableWorldReactions)
                .add()
                .append(new KeyedCodec<>("EnableBlockReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableBlockReactions = value,
                        config -> config.enableBlockReactions)
                .add()
                .build();
    }
}
