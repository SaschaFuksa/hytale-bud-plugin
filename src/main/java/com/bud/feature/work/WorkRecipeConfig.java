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
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.BudPlugin;
import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class WorkRecipeConfig {

    private static final String PACKAGED_FILE = "recipes.yml";

    private static WorkRecipeConfig instance;

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

    private final Set<String> diggableBlocks = new HashSet<>();

    @Nullable
    private String digRefillBlock;

    private final Map<Integer, Integer> treeGrowthStageSeconds = new java.util.HashMap<>();

    @Nullable
    private Integer treeGrowthDefaultSeconds;

    @Nonnull
    private static final String TREE_GROWTH_DEFAULT_KEY = "default";

    @Nonnull
    private static final List<String> ORE_BLOCK_SUFFIXES = Objects.requireNonNull(List.of(
            "_Stone", "_Magma", "_Slate", "_Shale", "_Sandstone", "_Basalt", "_Volcanic", "_Calcite", "_Mud"));

    @Nonnull
    private static final Set<String> LOGGED_UNRESOLVED_ORES = Objects
            .requireNonNull(java.util.Collections.synchronizedSet(new HashSet<>()));

    @Nonnull
    private Set<String> oreBlockIdCandidates = Objects.requireNonNull(Set.of());

    @Nonnull
    private final Map<String, String> resolvedOreTargetBlocks = new ConcurrentHashMap<>();

    private WorkRecipeConfig() {
    }

    public static WorkRecipeConfig getInstance() {
        if (instance == null) {
            instance = new WorkRecipeConfig();
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
        diggableBlocks.clear();
        digRefillBlock = null;
        treeGrowthStageSeconds.clear();
        treeGrowthDefaultSeconds = null;
        LOGGED_UNRESOLVED_ORES.clear();
        resolvedOreTargetBlocks.clear();
        if (!Files.exists(path)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Farming recipe file missing: " + path);
            return;
        }
        WorkRecipeYaml yaml = WorkRecipeYaml.load(path);
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
        diggableBlocks.addAll(yaml.getDiggableBlocks());
        if (diggableBlocks.isEmpty()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'diggableBlocks' in " + path + ".");
        }
        String refillBlock = yaml.getDigRefillBlock();
        if (refillBlock != null && !refillBlock.isBlank()) {
            digRefillBlock = refillBlock;
        } else {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No 'digRefillBlock' in " + path + ".");
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
        oreBlockIdCandidates = buildOreBlockIdCandidates();
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
                String prefix = Objects.requireNonNull(Objects.requireNonNullElse(pattern.get("prefix"), ""));
                String suffix = Objects.requireNonNull(Objects.requireNonNullElse(pattern.get("suffix"), ""));
                seedTargetPatternByRole.put(role, new SeedTargetPattern(prefix, suffix));
            } catch (IllegalArgumentException e) {
                String roleName = entry.getKey();
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Unknown WorkRole '" + roleName + "' in " + path + ", skipping.");
            }
        }
        seedTargetOverrides.putAll(yaml.getSeedTargetOverrides());
        for (Map.Entry<String, Integer> entry : yaml.getTreeGrowthStageSeconds().entrySet()) {
            Integer seconds = entry.getValue();
            if (seconds == null || seconds <= 0) {
                continue;
            }
            String key = entry.getKey().trim();
            if (TREE_GROWTH_DEFAULT_KEY.equalsIgnoreCase(key)) {
                treeGrowthDefaultSeconds = seconds;
                continue;
            }
            try {
                treeGrowthStageSeconds.put(Integer.valueOf(key), seconds);
            } catch (NumberFormatException e) {
                LoggerUtil.getLogger().warning(() -> "[BUD] 'treeGrowthStageSeconds' key '" + entry.getKey()
                        + "' in " + path + " is neither '" + TREE_GROWTH_DEFAULT_KEY
                        + "' nor a stage number, skipping.");
            }
        }
    }

    @Nullable
    public Integer getTreeGrowthStageSeconds(int stage) {
        Integer perStage = treeGrowthStageSeconds.get(stage);
        return perStage != null ? perStage : treeGrowthDefaultSeconds;
    }

    public boolean isSaplingBlock(@Nonnull String blockTypeId) {
        SeedTargetPattern pattern = seedTargetPatternByRole.get(WorkRole.LUMBERING);
        return pattern != null && !pattern.prefix().isEmpty() && blockTypeId.startsWith(pattern.prefix());
    }

    public boolean isBudSaplingBlock(@Nonnull String blockTypeId) {
        SeedTargetPattern pattern = seedTargetPatternByRole.get(WorkRole.LUMBERING);
        return pattern != null && !pattern.prefix().isEmpty() && blockTypeId.startsWith(pattern.prefix())
                && blockTypeId.endsWith(pattern.suffix());
    }

    public boolean hasTreeGrowthOverrides() {
        return treeGrowthDefaultSeconds != null || !treeGrowthStageSeconds.isEmpty();
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

    public boolean isDiggableBlock(@Nonnull String blockTypeId) {
        return diggableBlocks.contains(blockTypeId);
    }

    @Nullable
    public String getDigRefillBlock() {
        return digRefillBlock;
    }

    @Nullable
    public String getOreTargetBlock(@Nonnull String oreItemId) {
        if (!isSeedAllowed(WorkRole.MINING, oreItemId)) {
            return null;
        }
        String cachedCandidate = resolvedOreTargetBlocks.get(oreItemId);
        if (cachedCandidate != null) {
            return cachedCandidate;
        }
        for (String suffix : ORE_BLOCK_SUFFIXES) {
            String candidate = oreItemId + suffix;
            if (BlockType.fromString(candidate) != null) {
                resolvedOreTargetBlocks.put(oreItemId, candidate);
                if (LOGGED_UNRESOLVED_ORES.add(oreItemId)) {
                    LoggerUtil.getLogger()
                            .info(() -> "[BUD] Ore " + oreItemId + " grows as world block '" + candidate + "'.");
                }
                return candidate;
            }
        }
        if (LOGGED_UNRESOLVED_ORES.add(oreItemId)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Ore " + oreItemId + " has no world block in any known host "
                    + "rock " + ORE_BLOCK_SUFFIXES + " - Mining main nodes stay idle for it.");
        }
        return null;
    }

    public boolean isOreBlock(@Nonnull String blockTypeId) {
        return oreBlockIdCandidates.contains(blockTypeId);
    }

    @Nonnull
    private Set<String> buildOreBlockIdCandidates() {
        Set<String> candidates = new HashSet<>();
        for (String oreItemId : getAllowedSeeds(WorkRole.MINING)) {
            for (String suffix : ORE_BLOCK_SUFFIXES) {
                candidates.add(oreItemId + suffix);
            }
        }
        return Objects.requireNonNull(Set.copyOf(candidates));
    }

}
