package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ReactionConfig {

    public static final BuilderCodec<ReactionConfig> CODEC;

    private boolean enableCombatReactions = true;
    private boolean enableBlockReactions = true;
    private boolean enableItemReactions = true;
    private boolean enableDiscoverReactions = true;
    private boolean enableCraftingReactions = true;
    private boolean enableWorldReactions = true;
    private long worldReactionPeriodSeconds = 61L;
    private boolean enableWeatherReactions = true;
    private long weatherReactionPeriodSeconds = 6L;
    private boolean enableMoodReactions = true;
    private long moodReactionPeriodSeconds = 181L;
    private boolean enablePlayerChatReactions = true;
    private boolean enablePlayerStateReactions = true;
    private long playerStateReactionPeriodSeconds = 2L;
    private boolean enableWorkReactions = true;
    private long outputFullReactionPeriodSeconds = 300L;

    private static volatile ReactionConfig instance;

    public static void setInstance(ReactionConfig config) {
        instance = config;
    }

    public static ReactionConfig getInstance() {
        ReactionConfig config = instance;
        if (config == null) {
            instance = new ReactionConfig();
        }
        return instance;
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

    public long getWorldReactionPeriodSeconds() {
        return this.worldReactionPeriodSeconds;
    }

    public boolean isEnableWeatherReactions() {
        return this.enableWeatherReactions;
    }

    public long getWeatherReactionPeriodSeconds() {
        return this.weatherReactionPeriodSeconds;
    }

    public boolean isEnableMoodReactions() {
        return this.enableMoodReactions;
    }

    public long getMoodReactionPeriodSeconds() {
        return this.moodReactionPeriodSeconds;
    }

    public boolean isEnableItemReactions() {
        return this.enableItemReactions;
    }

    public boolean isEnableCraftingReactions() {
        return this.enableCraftingReactions;
    }

    public boolean isEnablePlayerChatReactions() {
        return this.enablePlayerChatReactions;
    }

    public boolean isEnablePlayerStateReactions() {
        return this.enablePlayerStateReactions;
    }

    public long getPlayerStateReactionPeriodSeconds() {
        return this.playerStateReactionPeriodSeconds;
    }

    public boolean isEnableWorkReactions() {
        return this.enableWorkReactions;
    }

    public long getOutputFullReactionPeriodSeconds() {
        return this.outputFullReactionPeriodSeconds;
    }

    static {
        CODEC = BuilderCodec.builder(ReactionConfig.class, ReactionConfig::new)
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
                        (config, value) -> config.worldReactionPeriodSeconds = value,
                        config -> config.worldReactionPeriodSeconds)
                .add()
                .append(new KeyedCodec<>("EnableWeatherReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableWeatherReactions = value,
                        config -> config.enableWeatherReactions)
                .add()
                .append(new KeyedCodec<>("WeatherReactionPeriod", Codec.LONG),
                        (config, value) -> config.weatherReactionPeriodSeconds = value,
                        config -> config.weatherReactionPeriodSeconds)
                .add()
                .append(new KeyedCodec<>("EnableMoodReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableMoodReactions = value,
                        config -> config.enableMoodReactions)
                .add()
                .append(new KeyedCodec<>("MoodReactionPeriod", Codec.LONG),
                        (config, value) -> config.moodReactionPeriodSeconds = value,
                        config -> config.moodReactionPeriodSeconds)
                .add()
                .append(new KeyedCodec<>("EnablePlayerChatReactions", Codec.BOOLEAN),
                        (config, value) -> config.enablePlayerChatReactions = value,
                        config -> config.enablePlayerChatReactions)
                .add()
                .append(new KeyedCodec<>("EnablePlayerStateReactions", Codec.BOOLEAN),
                        (config, value) -> config.enablePlayerStateReactions = value,
                        config -> config.enablePlayerStateReactions)
                .add()
                .append(new KeyedCodec<>("PlayerStateReactionPeriod", Codec.LONG),
                        (config, value) -> config.playerStateReactionPeriodSeconds = value,
                        config -> config.playerStateReactionPeriodSeconds)
                .add()
                .append(new KeyedCodec<>("EnableWorkReactions", Codec.BOOLEAN),
                        (config, value) -> config.enableWorkReactions = value,
                        config -> config.enableWorkReactions)
                .add()
                .append(new KeyedCodec<>("OutputFullReactionPeriod", Codec.LONG),
                        (config, value) -> config.outputFullReactionPeriodSeconds = value,
                        config -> config.outputFullReactionPeriodSeconds)
                .add()
                .build();
    }

}
