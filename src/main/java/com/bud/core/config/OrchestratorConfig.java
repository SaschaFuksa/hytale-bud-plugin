package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class OrchestratorConfig {

    public static final BuilderCodec<OrchestratorConfig> CODEC;

    private long orchestratorGlobalCooldownMs = 8000L; // ms between ANY bud message per player
    private long orchestratorChannelCooldownMs = 10000L; // ms between messages on the same channel
    private int orchestratorMaxQueueDepth = 3; // max pending events per channel per player
    private long orchestratorTickIntervalMs = 3000L; // how often the orchestrator checks queues
    private long orchestratorEntryTtlMs = 60000L; // max age of a queued entry before it is discarded

    private static volatile OrchestratorConfig instance;

    public static void setInstance(OrchestratorConfig config) {
        instance = config;
    }

    public static OrchestratorConfig getInstance() {
        OrchestratorConfig config = instance;
        if (config == null) {
            instance = new OrchestratorConfig();
        }
        return instance;
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

    public long getOrchestratorEntryTtlMs() {
        return this.orchestratorEntryTtlMs;
    }

    static {
        CODEC = BuilderCodec.builder(OrchestratorConfig.class, OrchestratorConfig::new)
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
                .append(new KeyedCodec<>("OrchestratorEntryTtlMs", Codec.LONG),
                        (config, value) -> config.orchestratorEntryTtlMs = value,
                        config -> config.orchestratorEntryTtlMs)
                .add()
                .build();
    }

}
