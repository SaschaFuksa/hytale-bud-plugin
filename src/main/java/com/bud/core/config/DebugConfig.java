package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DebugConfig {

    public static final BuilderCodec<DebugConfig> CODEC;

    private boolean enablePlayerInfo = false;
    private boolean enableBudDebugInfo = false;

    private static volatile DebugConfig instance;

    public static void setInstance(DebugConfig config) {
        instance = config;
    }

    public static DebugConfig getInstance() {
        DebugConfig config = instance;
        if (config == null) {
            instance = new DebugConfig();
        }
        return instance;
    }

    public boolean isEnablePlayerInfo() {
        return this.enablePlayerInfo;
    }

    public boolean isEnableBudDebugInfo() {
        return this.enableBudDebugInfo;
    }

    static {
        CODEC = BuilderCodec.builder(DebugConfig.class, DebugConfig::new)
                .append(new KeyedCodec<>("EnablePlayerInfo", Codec.BOOLEAN),
                        (config, value) -> config.enablePlayerInfo = value,
                        config -> config.enablePlayerInfo)
                .add()
                .append(new KeyedCodec<>("EnableBudDebugInfo", Codec.BOOLEAN),
                        (config, value) -> config.enableBudDebugInfo = value,
                        config -> config.enableBudDebugInfo)
                .add()
                .build();
    }

}
