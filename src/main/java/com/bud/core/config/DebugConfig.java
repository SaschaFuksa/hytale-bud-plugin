package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DebugConfig {

    public static final BuilderCodec<DebugConfig> CODEC;

    private boolean enablePlayerInfo = false;
    private boolean enableBudDebugInfo = false;
    private boolean enableMoodChangeDebugInfo = false;
    private String logLevel = "INFO"; // java.util.logging.Level name, e.g. ALL/FINE/INFO/WARNING
    private boolean autoUpdateContentOnVersionMismatch = true;
    // Gates `/bud state --working` (see StateCommand) - a debug-only path that sets WORKING without a
    // Workstation. Now that Workstation binding (Phase 4) is the real trigger, this must stay off by
    // default so WORKING isn't a normal, always-available player command.
    private boolean enableWorkingStateDebugCommand = false;

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

    public boolean isEnableMoodChangeDebugInfo() {
        return this.enableMoodChangeDebugInfo;
    }

    public String getLogLevel() {
        return this.logLevel;
    }

    public boolean isAutoUpdateContentOnVersionMismatch() {
        return this.autoUpdateContentOnVersionMismatch;
    }

    public boolean isEnableWorkingStateDebugCommand() {
        return this.enableWorkingStateDebugCommand;
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
                .append(new KeyedCodec<>("EnableMoodChangeDebugInfo", Codec.BOOLEAN),
                        (config, value) -> config.enableMoodChangeDebugInfo = value,
                        config -> config.enableMoodChangeDebugInfo)
                .add()
                .append(new KeyedCodec<>("LogLevel", Codec.STRING),
                        (config, value) -> config.logLevel = value,
                        config -> config.logLevel)
                .add()
                .append(new KeyedCodec<>("AutoUpdateContentOnVersionMismatch", Codec.BOOLEAN),
                        (config, value) -> config.autoUpdateContentOnVersionMismatch = value,
                        config -> config.autoUpdateContentOnVersionMismatch)
                .add()
                .append(new KeyedCodec<>("EnableWorkingStateDebugCommand", Codec.BOOLEAN),
                        (config, value) -> config.enableWorkingStateDebugCommand = value,
                        config -> config.enableWorkingStateDebugCommand)
                .add()
                .build();
    }

}
