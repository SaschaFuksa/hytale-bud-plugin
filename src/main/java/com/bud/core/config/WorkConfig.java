package com.bud.core.config;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.bud.core.types.FieldSize;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class WorkConfig {

    public static final BuilderCodec<WorkConfig> CODEC;

    private String farmingFieldSize = "LARGE";
    private String lumberingFieldSize = "LARGE";
    private String miningFieldSize = "LARGE";
    private int fieldMaxHeight = 2;
    private int targetTimeoutSeconds = 8;
    private int prepareSoilIntervalSeconds = 1;
    private int tillIntervalSeconds = 1;
    private int plantIntervalSeconds = 1;
    private int waterIntervalSeconds = 1;
    private int waterDurationSeconds = 86400;
    private int fertilizeIntervalSeconds = 1;
    private int harvestIntervalSeconds = 1;
    private int fellIntervalSeconds = 1;
    private int idleRetrySeconds = 5;
    private int treeMinDistance = 3;
    private int treeRootDepth = 4;
    private int treeRootRadius = 1;
    private int oreMinDistance = 2;
    private int fuelDurationSeconds = 300;
    private int rebindRetrySeconds = 10;
    private int miningGrowthGameSecondsMin = 5000;
    private int miningGrowthGameSecondsMax = 5100;
    private int digIntervalSeconds = 1;
    private int mineIntervalSeconds = 1;
    private int decorateIntervalSeconds = 1;

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

    @Nonnull
    public FieldSize getFieldSize(@Nonnull WorkRole workRole) {
        String raw = switch (workRole) {
            case FARMING -> farmingFieldSize;
            case LUMBERING -> lumberingFieldSize;
            case MINING -> miningFieldSize;
            default -> null;
        };
        if (raw == null || raw.isBlank()) {
            return FieldSize.MEDIUM;
        }
        try {
            return FieldSize.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Unknown field size '" + raw + "' for " + workRole
                    + " - falling back to MEDIUM.");
            return FieldSize.MEDIUM;
        }
    }

    public int getFieldRadius(@Nonnull WorkRole workRole) {
        FieldSize size = getFieldSize(workRole);
        if (workRole == WorkRole.FARMING) {
            return switch (size) {
                case SMALL -> 4;
                case MEDIUM -> 5;
                case LARGE -> 6;
            };
        }
        return switch (size) {
            case SMALL -> 3;
            case MEDIUM -> 5;
            case LARGE -> 7;
        };
    }

    public int getFieldStructureCount(@Nonnull WorkRole workRole) {
        return switch (getFieldSize(workRole)) {
            case SMALL -> 2;
            case MEDIUM -> 4;
            case LARGE -> 8;
        };
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

    public int getFertilizeIntervalSeconds() {
        return this.fertilizeIntervalSeconds;
    }

    public int getHarvestIntervalSeconds() {
        return this.harvestIntervalSeconds;
    }

    public int getFellIntervalSeconds() {
        return this.fellIntervalSeconds;
    }

    public int getIdleRetrySeconds() {
        return this.idleRetrySeconds;
    }

    public int getTreeMinDistance() {
        return this.treeMinDistance;
    }

    public int getTreeRootDepth() {
        return this.treeRootDepth;
    }

    public int getTreeRootRadius() {
        return this.treeRootRadius;
    }

    public int getPrepareSoilIntervalSeconds() {
        return this.prepareSoilIntervalSeconds;
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

    public int getMiningGrowthGameSecondsMin() {
        return this.miningGrowthGameSecondsMin;
    }

    public int getMiningGrowthGameSecondsMax() {
        return this.miningGrowthGameSecondsMax;
    }

    public int getDigIntervalSeconds() {
        return this.digIntervalSeconds;
    }

    public int getMineIntervalSeconds() {
        return this.mineIntervalSeconds;
    }

    public int getDecorateIntervalSeconds() {
        return this.decorateIntervalSeconds;
    }

    static {
        CODEC = BuilderCodec.builder(WorkConfig.class, WorkConfig::new)
                .append(new KeyedCodec<>("FarmingFieldSize", Codec.STRING),
                        (config, value) -> config.farmingFieldSize = value,
                        config -> config.farmingFieldSize)
                .add()
                .append(new KeyedCodec<>("LumberingFieldSize", Codec.STRING),
                        (config, value) -> config.lumberingFieldSize = value,
                        config -> config.lumberingFieldSize)
                .add()
                .append(new KeyedCodec<>("MiningFieldSize", Codec.STRING),
                        (config, value) -> config.miningFieldSize = value,
                        config -> config.miningFieldSize)
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
                .append(new KeyedCodec<>("FertilizeIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.fertilizeIntervalSeconds = value,
                        config -> config.fertilizeIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("HarvestIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.harvestIntervalSeconds = value,
                        config -> config.harvestIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("FellIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.fellIntervalSeconds = value,
                        config -> config.fellIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("IdleRetrySeconds", Codec.INTEGER),
                        (config, value) -> config.idleRetrySeconds = value,
                        config -> config.idleRetrySeconds)
                .add()
                .append(new KeyedCodec<>("TreeMinDistance", Codec.INTEGER),
                        (config, value) -> config.treeMinDistance = value,
                        config -> config.treeMinDistance)
                .add()
                .append(new KeyedCodec<>("TreeRootDepth", Codec.INTEGER),
                        (config, value) -> config.treeRootDepth = value,
                        config -> config.treeRootDepth)
                .add()
                .append(new KeyedCodec<>("TreeRootRadius", Codec.INTEGER),
                        (config, value) -> config.treeRootRadius = value,
                        config -> config.treeRootRadius)
                .add()
                .append(new KeyedCodec<>("PrepareSoilIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.prepareSoilIntervalSeconds = value,
                        config -> config.prepareSoilIntervalSeconds)
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
                .append(new KeyedCodec<>("MiningGrowthGameSecondsMin", Codec.INTEGER),
                        (config, value) -> config.miningGrowthGameSecondsMin = value,
                        config -> config.miningGrowthGameSecondsMin)
                .add()
                .append(new KeyedCodec<>("MiningGrowthGameSecondsMax", Codec.INTEGER),
                        (config, value) -> config.miningGrowthGameSecondsMax = value,
                        config -> config.miningGrowthGameSecondsMax)
                .add()
                .append(new KeyedCodec<>("DigIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.digIntervalSeconds = value,
                        config -> config.digIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("MineIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.mineIntervalSeconds = value,
                        config -> config.mineIntervalSeconds)
                .add()
                .append(new KeyedCodec<>("DecorateIntervalSeconds", Codec.INTEGER),
                        (config, value) -> config.decorateIntervalSeconds = value,
                        config -> config.decorateIntervalSeconds)
                .add()
                .build();
    }

}
