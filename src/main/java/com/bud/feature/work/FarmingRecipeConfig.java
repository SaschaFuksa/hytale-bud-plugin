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

public final class FarmingRecipeConfig {

    private static final String PACKAGED_FILE = "farming.yml";

    private static FarmingRecipeConfig instance;

    private final Map<WorkRole, Set<String>> allowedSeedsByRole = new EnumMap<>(WorkRole.class);

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
        if (!Files.exists(path)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Farming recipe file missing: " + path);
            return;
        }
        FarmingRecipeYaml yaml = FarmingRecipeYaml.load(path);
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

}
