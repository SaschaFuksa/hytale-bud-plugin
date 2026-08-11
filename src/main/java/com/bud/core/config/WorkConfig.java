package com.bud.core.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class WorkConfig {

    public static final BuilderCodec<WorkConfig> CODEC;

    private int fieldRadius = 1;
    private int treeMinDistance = 3;
    private int oreMinDistance = 2;
    private int fuelDurationSeconds = 600;

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

    public int getTreeMinDistance() {
        return this.treeMinDistance;
    }

    public int getOreMinDistance() {
        return this.oreMinDistance;
    }

    public int getFuelDurationSeconds() {
        return this.fuelDurationSeconds;
    }

    static {
        CODEC = BuilderCodec.builder(WorkConfig.class, WorkConfig::new)
                .append(new KeyedCodec<>("FieldRadius", Codec.INTEGER),
                        (config, value) -> config.fieldRadius = value,
                        config -> config.fieldRadius)
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
                .build();
    }

}
