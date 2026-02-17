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
    private int maxTokens = 100;
    private double temperature = 0.9;
    private boolean enableCombatReactions = true;
    private boolean enableBlockReactions = true;
    private boolean enableItemReactions = true;
    private boolean enableDiscoverReactions = true;
    private boolean enableCraftingReactions = true;
    private boolean enableWorldReactions = true;
    private long worldReactionPeriod = 60L; // seconds
    private boolean enableWeatherReactions = true;
    private long weatherReactionPeriod = 5L; // seconds
    private boolean enableMoodReactions = true;
    private long moodReactionPeriod = 180L; // seconds

    // Orchestrator settings
    private long orchestratorGlobalCooldownMs = 3000L; // ms between ANY bud message per player
    private long orchestratorChannelCooldownMs = 5000L; // ms between messages on the same channel
    private int orchestratorMaxQueueDepth = 3; // max pending events per channel per player
    private long orchestratorTickIntervalMs = 1000L; // how often the orchestrator checks queues

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

    public boolean isEnableBlockReactions() {
        return this.enableBlockReactions;
    }

    public boolean isEnableDiscoverReactions() {
        return this.enableDiscoverReactions;
    }

    public boolean isEnableWorldReactions() {
        return this.enableWorldReactions;
    }

    public long getWorldReactionPeriod() {
        return this.worldReactionPeriod;
    }

    public boolean isEnableWeatherReactions() {
        return this.enableWeatherReactions;
    }

    public long getWeatherReactionPeriod() {
        return this.weatherReactionPeriod;
    }

    public boolean isEnableMoodReactions() {
        return this.enableMoodReactions;
    }

    public long getMoodReactionPeriod() {
        return this.moodReactionPeriod;
    }

    public boolean isEnableItemReactions() {
        return this.enableItemReactions;
    }

    public boolean isEnableCraftingReactions() {
        return this.enableCraftingReactions;
    }

    public long getOrchestratorGlobalCooldownMs() {
        return this.orchestratorGlobalCooldownMs;
    }

    public long getOrchestratorChannelCooldownMs() {
        return this.orchestratorChannelCooldownMs;
    }

    public int getOrchestratorMaxQueueDepth() {
        return this.orchestratorMaxQueueDepth;
    }

    public long getOrchestratorTickIntervalMs() {
        return this.orchestratorTickIntervalMs;
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
                .append(new KeyedCodec<>("EnableBlockReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableBlockReactions = value,
                        config -> config.enableBlockReactions)
                .add()
                .append(new KeyedCodec<>("EnableItemReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableItemReactions = value,
                        config -> config.enableItemReactions)
                .add()
                .append(new KeyedCodec<>("EnableDiscoverReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableDiscoverReactions = value,
                        config -> config.enableDiscoverReactions)
                .add()
                .append(new KeyedCodec<>("EnableCraftingReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableCraftingReactions = value,
                        config -> config.enableCraftingReactions)
                .add()
                .append(new KeyedCodec<>("EnableWorldReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableWorldReactions = value,
                        config -> config.enableWorldReactions)
                .add()
                .append(new KeyedCodec<>("WorldReactionPeriod", Codec.LONG),
                        (config, value) -> config.worldReactionPeriod = value,
                        config -> config.worldReactionPeriod)
                .add()
                .append(new KeyedCodec<>("EnableWeatherReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableWeatherReactions = value,
                        config -> config.enableWeatherReactions)
                .add()
                .append(new KeyedCodec<>("WeatherReactionPeriod", Codec.LONG),
                        (config, value) -> config.weatherReactionPeriod = value,
                        config -> config.weatherReactionPeriod)
                .add()
                .append(new KeyedCodec<>("EnableMoodReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableMoodReactions = value,
                        config -> config.enableMoodReactions)
                .add()
                .append(new KeyedCodec<>("MoodReactionPeriod", Codec.LONG),
                        (config, value) -> config.moodReactionPeriod = value,
                        config -> config.moodReactionPeriod)
                .add()
                .append(new KeyedCodec<>("OrchestratorGlobalCooldownMs", Codec.LONG),
                        (config, value) -> config.orchestratorGlobalCooldownMs = value,
                        config -> config.orchestratorGlobalCooldownMs)
                .add()
                .append(new KeyedCodec<>("OrchestratorChannelCooldownMs", Codec.LONG),
                        (config, value) -> config.orchestratorChannelCooldownMs = value,
                        config -> config.orchestratorChannelCooldownMs)
                .add()
                .append(new KeyedCodec<>("OrchestratorMaxQueueDepth", Codec.INTEGER),
                        (config, value) -> config.orchestratorMaxQueueDepth = value,
                        config -> config.orchestratorMaxQueueDepth)
                .add()
                .append(new KeyedCodec<>("OrchestratorTickIntervalMs", Codec.LONG),
                        (config, value) -> config.orchestratorTickIntervalMs = value,
                        config -> config.orchestratorTickIntervalMs)
                .add()
                .build();
    }
}
