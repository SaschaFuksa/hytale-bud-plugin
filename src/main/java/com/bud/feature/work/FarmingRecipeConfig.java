package com.bud.feature.work;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.BudPlugin;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class FarmingRecipeConfig {

    private static final String PACKAGED_FILE = "farming.yml";

    private static FarmingRecipeConfig instance;

    private final Map<WorkRole, Set<String>> allowedSeedsByRole = new EnumMap<>(WorkRole.class);

    private final Map<WorkRole, Set<String>> allowedFuelByRole = new EnumMap<>(WorkRole.class);

    private final Map<WorkRole, SeedTargetPattern> seedTargetPatternByRole = new EnumMap<>(WorkRole.class);

    record SeedTargetPattern(@Nonnull String prefix, @Nonnull String suffix) {
    }

    private final Map<String, String> seedTargetOverrides = new java.util.HashMap<>();

    private final Set<String> tillableBlocks = new HashSet<>();

    private final Set<String> tilledSoilBlocks = new HashSet<>();

    @Nullable
    private String tilledSoilTargetBlock;

    private FarmingRecipeConfig() {
    }

    public static FarmingRecipeConfig getInstance() {
        if (instance == null) {
            instance = new FarmingRecipeConfig();
        }
        return instance;
    }

    public void reloadMissing() {
        Path rootDataDir = BudPlugin.getInstance().getDataDirectory();
        Path workDir = Objects.requireNonNull(rootDataDir.resolve("work"));
        copyPackagedDefault(workDir);
        load(Objects.requireNonNull(workDir.resolve(PACKAGED_FILE)));
    }

    private void copyPackagedDefault(@Nonnull Path workDir) {
        Path target = workDir.resolve(PACKAGED_FILE);
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(workDir);
            try (InputStream in = BudPlugin.class.getResourceAsStream("/work/" + PACKAGED_FILE)) {
                if (in == null) {
                    LoggerUtil.getLogger()
                            .severe(() -> "[BUD] Default work resource not found in JAR: /work/" + PACKAGED_FILE);
                    return;
                }
                Files.copy(in, target);
                LoggerUtil.getLogger().info(() -> "[BUD] Work resource created: " + target);
            }
        } catch (IOException e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Failed to copy default work resource: " + PACKAGED_FILE + " - "
                            + e.getMessage());
        }
    }

    private void load(@Nonnull Path path) {
        allowedSeedsByRole.clear();
        allowedFuelByRole.clear();
        seedTargetPatternByRole.clear();
        seedTargetOverrides.clear();
        tillableBlocks.clear();
        tilledSoilBlocks.clear();
        tilledSoilTargetBlock = null;
        if (!Files.exists(path)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Farming recipe file missing: " + path);
            return;
        }
        FarmingRecipeYaml yaml = FarmingRecipeYaml.load(path);
        tillableBlocks.addAll(yaml.getTillableBlocks());
        if (tillableBlocks.isEmpty()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'tillableBlocks' in " + path + ".");
        }
        tilledSoilBlocks.addAll(yaml.getTilledSoilBlocks());
        if (tilledSoilBlocks.isEmpty()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'tilledSoilBlocks' in " + path + ".");
        }
        String targetBlock = yaml.getTilledSoilTargetBlock();
        if (targetBlock != null && !targetBlock.isBlank()) {
            tilledSoilTargetBlock = targetBlock;
        } else {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'tilledSoilTargetBlock' in " + path + ".");
        }
        for (Map.Entry<String, List<String>> entry : yaml.getAllowedSeeds().entrySet()) {
            try {
                WorkRole role = WorkRole.valueOf(entry.getKey());
                allowedSeedsByRole.put(role, new HashSet<>(entry.getValue()));
            } catch (IllegalArgumentException e) {
                String roleName = entry.getKey();
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Unknown WorkRole '" + roleName + "' in " + path + ", skipping.");
            }
        }
        for (Map.Entry<String, List<String>> entry : yaml.getAllowedFuel().entrySet()) {
            try {
                WorkRole role = WorkRole.valueOf(entry.getKey());
                allowedFuelByRole.put(role, new HashSet<>(entry.getValue()));
            } catch (IllegalArgumentException e) {
                String roleName = entry.getKey();
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Unknown WorkRole '" + roleName + "' in " + path + ", skipping.");
            }
        }
        for (Map.Entry<String, Map<String, String>> entry : yaml.getSeedTargetPattern().entrySet()) {
            try {
                WorkRole role = WorkRole.valueOf(entry.getKey());
                Map<String, String> pattern = entry.getValue();
                String prefix = Objects.requireNonNullElse(pattern.get("prefix"), "");
                String suffix = Objects.requireNonNullElse(pattern.get("suffix"), "");
                seedTargetPatternByRole.put(role, new SeedTargetPattern(prefix, suffix));
            } catch (IllegalArgumentException e) {
                String roleName = entry.getKey();
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Unknown WorkRole '" + roleName + "' in " + path + ", skipping.");
            }
        }
        seedTargetOverrides.putAll(yaml.getSeedTargetOverrides());
    }

    @Nonnull
    public Set<String> getAllowedSeeds(@Nonnull WorkRole workRole) {
        return Objects.requireNonNull(allowedSeedsByRole.getOrDefault(workRole, Set.of()));
    }

    public boolean isSeedAllowed(@Nonnull WorkRole workRole, @Nonnull String seedItemId) {
        return getAllowedSeeds(workRole).contains(seedItemId);
    }

    @Nonnull
    public Set<String> getAllowedFuel(@Nonnull WorkRole workRole) {
        return Objects.requireNonNull(allowedFuelByRole.getOrDefault(workRole, Set.of()));
    }

    public boolean isFuelAllowed(@Nonnull WorkRole workRole, @Nonnull String fuelItemId) {
        return getAllowedFuel(workRole).contains(fuelItemId);
    }

    @Nullable
    SeedTargetPattern getSeedTargetPattern(@Nonnull WorkRole workRole) {
        return seedTargetPatternByRole.get(workRole);
    }

    @Nullable
    String getSeedTargetOverride(@Nonnull String seedItemId) {
        return seedTargetOverrides.get(seedItemId);
    }

    public boolean isTillableBlock(@Nonnull String blockTypeId) {
        return tillableBlocks.contains(blockTypeId);
    }

    public boolean isTilledSoilBlock(@Nonnull String blockTypeId) {
        if (tilledSoilBlocks.contains(blockTypeId)) {
            return true;
        }
        if (tilledSoilTargetBlock == null) {
            return false;
        }
        BlockType base = BlockType.fromString(tilledSoilTargetBlock);
        return base != null && base.getStateForBlock(blockTypeId) != null;
    }

    @Nullable
    public String getTilledSoilTargetBlock() {
        return tilledSoilTargetBlock;
    }

}
