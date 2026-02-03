package com.bud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class BudConfig {
    public static final BuilderCodec<BudConfig> CODEC;

    private boolean enableLLM = true;
    private boolean usePlayer2API = true;
    private String url = "http://192.168.178.25:1234/v1/chat/completions";
    private String model = "mistralai/ministral-3-3b";
    private String apiKey = "not_needed";

    private static volatile BudConfig instance;

    public static void setInstance(BudConfig config) {
        instance = config;
    }

    public static BudConfig get() {
        BudConfig config = instance;
        if (config == null) {
            throw new IllegalStateException("BudConfig not yet initialized. Ensure BudPlugin.setup() has been called.");
        }
        return config;
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

    static {
        CODEC = (BuilderCodec<BudConfig>) BuilderCodec.builder(BudConfig.class, BudConfig::new)
                .append(new KeyedCodec("EnableLLM", Codec.BOOLEAN), (config, value, extra) -> {
                    ((BudConfig) config).enableLLM = value;
                }, (config, extra) -> {
                    return ((BudConfig) config).enableLLM;
                }).add()
                .append(new KeyedCodec("UsePlayer2API", Codec.BOOLEAN), (config, value, extra) -> {
                    ((BudConfig) config).usePlayer2API = (boolean) value;
                }, (config, extra) -> {
                    return ((BudConfig) config).usePlayer2API;
                }).add()
                .append(new KeyedCodec("Url", Codec.STRING), (config, value, extra) -> {
                    ((BudConfig) config).url = (String) value;
                }, (config, extra) -> {
                    return ((BudConfig) config).url;
                }).add()
                .append(new KeyedCodec("Model", Codec.STRING), (config, value, extra) -> {
                    ((BudConfig) config).model = (String) value;
                }, (config, extra) -> {
                    return ((BudConfig) config).model;
                }).add()
                .append(new KeyedCodec("ApiKey", Codec.STRING), (config, value, extra) -> {
                    ((BudConfig) config).apiKey = (String) value;
                }, (config, extra) -> {
                    return ((BudConfig) config).apiKey;
                }).add()
                .build();
    }
}
