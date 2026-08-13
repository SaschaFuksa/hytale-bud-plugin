package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class WorkConfig {

    public static final BuilderCodec<WorkConfig> CODEC;

    private int fieldRadius = 5;
    private int fieldMaxHeight = 3;
    private int targetTimeoutSeconds = 8;
    private int tillIntervalSeconds = 1;
    private int plantIntervalSeconds = 1;
    private int waterIntervalSeconds = 1;
    private int waterDurationSeconds = 600;
    private int idleRetrySeconds = 5;
    private int treeMinDistance = 3;
    private int oreMinDistance = 2;
    private int fuelDurationSeconds = 120;
    private int rebindRetrySeconds = 10;

    private static volatile WorkConfig instance;

    public static void setInstance(WorkConfig config) {
        instance = config;
    }

    public static WorkConfig getInstance() {
        WorkConfig config = instance;
        if (config == null) {
            instance = new WorkConfig();
        }
        return instance;
    }

    public int getFieldRadius() {
        return this.fieldRadius;
    }

    public int getFieldMaxHeight() {
        return this.fieldMaxHeight;
    }

    public int getTargetTimeoutSeconds() {
        return this.targetTimeoutSeconds;
    }

    public int getTillIntervalSeconds() {
        return this.tillIntervalSeconds;
    }

    public int getPlantIntervalSeconds() {
        return this.plantIntervalSeconds;
    }

    public int getWaterIntervalSeconds() {
        return this.waterIntervalSeconds;
    }

    public int getWaterDurationSeconds() {
        return this.waterDurationSeconds;
    }

    public int getIdleRetrySeconds() {
        return this.idleRetrySeconds;
    }

    public int getTreeMinDistance() {
        return this.treeMinDistance;
    }

    public int getOreMinDistance() {
        return this.oreMinDistance;
    }

    public int getFuelDurationSeconds() {
        return this.fuelDurationSeconds;
    }

    public int getRebindRetrySeconds() {
        return this.rebindRetrySeconds;
    }

    static {
        CODEC = BuilderCodec.builder(WorkConfig.class, WorkConfig::new)
                .append(new KeyedCodec<>("FieldRadius", Codec.INTEGER),
                        (config, value) -> config.fieldRadius = value,
                        config -> config.fieldRadius)
                .add()
                .append(new KeyedCodec<>("FieldMaxHeight", Codec.INTEGER),
                        (config, value) -> config.fieldMaxHeight = value,
                        config -> config.fieldMaxHeight)
                .add()
                .append(new KeyedCodec<>("TargetTimeoutSeconds", Codec.INTEGER),
                        (config, value) -> config.targetTimeoutSeconds = value,
                        config -> config.targetTimeoutSeconds)
                .add()
                .append(new KeyedCodec<>("TillIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.tillIntervalSeconds = value,
                        config -> config.tillIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("PlantIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.plantIntervalSeconds = value,
                        config -> config.plantIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("WaterIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.waterIntervalSeconds = value,
                        config -> config.waterIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("WaterDurationSeconds", Codec.INTEGER),
                        (config, value) -> config.waterDurationSeconds = value,
                        config -> config.waterDurationSeconds)
                .add()
                .append(new KeyedCodec<>("IdleRetrySeconds", Codec.INTEGER),
                        (config, value) -> config.idleRetrySeconds = value,
                        config -> config.idleRetrySeconds)
                .add()
                .append(new KeyedCodec<>("TreeMinDistance", Codec.INTEGER),
                        (config, value) -> config.treeMinDistance = value,
                        config -> config.treeMinDistance)
                .add()
                .append(new KeyedCodec<>("OreMinDistance", Codec.INTEGER),
                        (config, value) -> config.oreMinDistance = value,
                        config -> config.oreMinDistance)
                .add()
                .append(new KeyedCodec<>("FuelDurationSeconds", Codec.INTEGER),
                        (config, value) -> config.fuelDurationSeconds = value,
                        config -> config.fuelDurationSeconds)
                .add()
                .append(new KeyedCodec<>("RebindRetrySeconds", Codec.INTEGER),
                        (config, value) -> config.rebindRetrySeconds = value,
                        config -> config.rebindRetrySeconds)
                .add()
                .build();
    }

}
