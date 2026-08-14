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

import com.bud.BudPlugin;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class FarmingRecipeConfig {

    private static final String PACKAGED_FILE = "farming.yml";

    private static FarmingRecipeConfig instance;

    private static final Set<String> DEFAULT_TILLABLE_BLOCKS = Objects.requireNonNull(Set.of(
            "Soil_Dirt", "Soil_Dirt_Burnt", "Soil_Dirt_Cold", "Soil_Dirt_Dry",
            "Soil_Grass", "Soil_Grass_Burnt", "Soil_Grass_Cold", "Soil_Grass_Deep",
            "Soil_Grass_Dry", "Soil_Grass_Full", "Soil_Grass_Sunny", "Soil_Leaves",
            "Soil_Mud", "Soil_Mud_Dry", "Soil_Needles", "Soil_Pathway"));

    private static final Set<String> DEFAULT_TILLED_SOIL_BLOCKS = Objects.requireNonNull(Set.of(
            "Soil_Dirt_Tilled",
            "*Soil_Dirt_Tilled_State_Definitions_Watered",
            "*Soil_Dirt_Tilled_State_Definitions_Fertilized",
            "*Soil_Dirt_Tilled_State_Definitions_Fertilized_Watered"));

    private static final String DEFAULT_TILLED_SOIL_TARGET_BLOCK = "Soil_Dirt_Tilled";

    private final Map<WorkRole, Set<String>> allowedSeedsByRole = new EnumMap<>(WorkRole.class);

    private final Set<String> tillableBlocks = new HashSet<>();

    private final Set<String> tilledSoilBlocks = new HashSet<>();

    private String tilledSoilTargetBlock = DEFAULT_TILLED_SOIL_TARGET_BLOCK;

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
        tillableBlocks.clear();
        tilledSoilBlocks.clear();
        tilledSoilTargetBlock = DEFAULT_TILLED_SOIL_TARGET_BLOCK;
        if (!Files.exists(path)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Farming recipe file missing: " + path);
            tillableBlocks.addAll(DEFAULT_TILLABLE_BLOCKS);
            tilledSoilBlocks.addAll(DEFAULT_TILLED_SOIL_BLOCKS);
            return;
        }
        FarmingRecipeYaml yaml = FarmingRecipeYaml.load(path);
        tillableBlocks.addAll(yaml.getTillableBlocks());
        if (tillableBlocks.isEmpty()) {
            tillableBlocks.addAll(DEFAULT_TILLABLE_BLOCKS);
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'tillableBlocks' in " + path + ", using built-in defaults.");
        }
        tilledSoilBlocks.addAll(yaml.getTilledSoilBlocks());
        if (tilledSoilBlocks.isEmpty()) {
            tilledSoilBlocks.addAll(DEFAULT_TILLED_SOIL_BLOCKS);
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'tilledSoilBlocks' in " + path + ", using built-in defaults.");
        }
        String targetBlock = yaml.getTilledSoilTargetBlock();
        if (targetBlock != null && !targetBlock.isBlank()) {
            tilledSoilTargetBlock = targetBlock;
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
    }

    @Nonnull
    public Set<String> getAllowedSeeds(@Nonnull WorkRole workRole) {
        return Objects.requireNonNull(allowedSeedsByRole.getOrDefault(workRole, Set.of()));
    }

    public boolean isSeedAllowed(@Nonnull WorkRole workRole, @Nonnull String seedItemId) {
        return getAllowedSeeds(workRole).contains(seedItemId);
    }

    public boolean isTillableBlock(@Nonnull String blockTypeId) {
        return tillableBlocks.contains(blockTypeId);
    }

    /**
     * State changes swap the block's own id (a watered {@code Soil_Dirt_Tilled} reports itself as
     * {@code *Soil_Dirt_Tilled_State_Definitions_Watered}, bytecode-confirmed via
     * {@code BlockType.getStateForBlock}), so a plain id comparison against the base id alone silently
     * drops every watered/fertilized tile from every work type - the bug this replaces (see
     * docs/bud-worker-mode-plan.md, "Phase 6, Zustandsvarianten-Bug"). {@code tilledSoilTargetBlock}'s
     * own {@link BlockType#getStateForBlock} is the SDK-native way to recognize any of its state
     * variants without enumerating them - covers a future Hytale update adding a new state to
     * {@code Soil_Dirt_Tilled} automatically, no config change needed. {@code tilledSoilBlocks} stays
     * checked first as an explicit override/extension point (e.g. a custom mod block that should count
     * as tilled soil without being a native state of it).
     */
    public boolean isTilledSoilBlock(@Nonnull String blockTypeId) {
        if (tilledSoilBlocks.contains(blockTypeId)) {
            return true;
        }
        BlockType base = BlockType.fromString(Objects.requireNonNull(tilledSoilTargetBlock));
        return base != null && base.getStateForBlock(blockTypeId) != null;
    }

    @Nonnull
    public String getTilledSoilTargetBlock() {
        return Objects.requireNonNull(tilledSoilTargetBlock);
    }

}
