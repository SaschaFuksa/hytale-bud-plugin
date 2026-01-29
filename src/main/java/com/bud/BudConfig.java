package com.bud;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class BudConfig {
    public static final BuilderCodec<BudConfig> CODEC;
    
    private boolean enableLLM = true;
    private String url = "http://192.168.178.25:1234/v1/chat/completions";
    private String model = "mistralai/ministral-3-3b";
    private String apiKey = "not_needed";

    public BudConfig() {
    }

    public boolean isEnableLLM() {
        return this.enableLLM;
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
                    ((BudConfig)config).enableLLM = (Boolean)value;
                }, (config, extra) -> {
                    return ((BudConfig)config).enableLLM;
                }).add()
                .append(new KeyedCodec("Url", Codec.STRING), (config, value, extra) -> {
                    ((BudConfig)config).url = (String)value;
                }, (config, extra) -> {
                    return ((BudConfig)config).url;
                }).add()
                .append(new KeyedCodec("Model", Codec.STRING), (config, value, extra) -> {
                    ((BudConfig)config).model = (String)value;
                }, (config, extra) -> {
                    return ((BudConfig)config).model;
                }).add()
                .append(new KeyedCodec("ApiKey", Codec.STRING), (config, value, extra) -> {
                    ((BudConfig)config).apiKey = (String)value;
                }, (config, extra) -> {
                    return ((BudConfig)config).apiKey;
                }).add()
                .build();
    }
}
