package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ConversationConfig {

    public static final BuilderCodec<ConversationConfig> CODEC;

    private boolean enableConversationMemory = true;
    private int conversationMemoryDepth = 8;
    private double conversationMemoryDecayFactor = 0.9;
    private int conversationMemoryMinImportance = 4;
    private boolean enableLegendaryMemory = true;
    private int legendaryMemorySlotsPerBud = 3;
    private boolean enableDialogMode = true;
    private long dialogModeIdleSeconds = 180L;
    private long dialogModeActiveSeconds = 30L;
    private long dialogModeTurnIntervalSeconds = 8L;

    private static volatile ConversationConfig instance;

    public static void setInstance(ConversationConfig config) {
        instance = config;
    }

    public static ConversationConfig getInstance() {
        ConversationConfig config = instance;
        if (config == null) {
            instance = new ConversationConfig();
        }
        return instance;
    }

    public boolean isEnableConversationMemory() {
        return this.enableConversationMemory;
    }

    public int getConversationMemoryDepth() {
        return this.conversationMemoryDepth;
    }

    public double getConversationMemoryDecayFactor() {
        return this.conversationMemoryDecayFactor;
    }

    public int getConversationMemoryMinImportance() {
        return this.conversationMemoryMinImportance;
    }

    public boolean isEnableLegendaryMemory() {
        return this.enableLegendaryMemory;
    }

    public int getLegendaryMemorySlotsPerBud() {
        return this.legendaryMemorySlotsPerBud;
    }

    public boolean isEnableDialogMode() {
        return this.enableDialogMode;
    }

    public long getDialogModeIdleSeconds() {
        return this.dialogModeIdleSeconds;
    }

    public long getDialogModeActiveSeconds() {
        return this.dialogModeActiveSeconds;
    }

    public long getDialogModeTurnIntervalSeconds() {
        return this.dialogModeTurnIntervalSeconds;
    }

    static {
        CODEC = BuilderCodec.builder(ConversationConfig.class, ConversationConfig::new)
                .append(new KeyedCodec<>("EnableConversationMemory", Codec.BOOLEAN),
                        (config, value) -> config.enableConversationMemory = value,
                        config -> config.enableConversationMemory)
                .add()
                .append(new KeyedCodec<>("ConversationMemoryDepth", Codec.INTEGER),
                        (config, value) -> config.conversationMemoryDepth = value,
                        config -> config.conversationMemoryDepth)
                .add()
                .append(new KeyedCodec<>("ConversationMemoryDecayFactor", Codec.DOUBLE),
                        (config, value) -> config.conversationMemoryDecayFactor = value,
                        config -> config.conversationMemoryDecayFactor)
                .add()
                .append(new KeyedCodec<>("ConversationMemoryMinImportance", Codec.INTEGER),
                        (config, value) -> config.conversationMemoryMinImportance = value,
                        config -> config.conversationMemoryMinImportance)
                .add()
                .append(new KeyedCodec<>("EnableLegendaryMemory", Codec.BOOLEAN),
                        (config, value) -> config.enableLegendaryMemory = value,
                        config -> config.enableLegendaryMemory)
                .add()
                .append(new KeyedCodec<>("LegendaryMemorySlotsPerBud", Codec.INTEGER),
                        (config, value) -> config.legendaryMemorySlotsPerBud = value,
                        config -> config.legendaryMemorySlotsPerBud)
                .add()
                .append(new KeyedCodec<>("EnableDialogMode", Codec.BOOLEAN),
                        (config, value) -> config.enableDialogMode = value,
                        config -> config.enableDialogMode)
                .add()
                .append(new KeyedCodec<>("DialogModeIdleSeconds", Codec.LONG),
                        (config, value) -> config.dialogModeIdleSeconds = value,
                        config -> config.dialogModeIdleSeconds)
                .add()
                .append(new KeyedCodec<>("DialogModeActiveSeconds", Codec.LONG),
                        (config, value) -> config.dialogModeActiveSeconds = value,
                        config -> config.dialogModeActiveSeconds)
                .add()
                .append(new KeyedCodec<>("DialogModeTurnIntervalSeconds", Codec.LONG),
                        (config, value) -> config.dialogModeTurnIntervalSeconds = value,
                        config -> config.dialogModeTurnIntervalSeconds)
                .add()
                .build();
    }

}