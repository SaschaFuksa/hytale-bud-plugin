package com.bud.core.registry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.BudPlugin;
import com.bud.core.config.DebugConfig;
import com.bud.core.content.ContentVersion;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class BudRegistry {

    private static final String[] PACKAGED_DEFINITIONS = { "veri.yml", "keyleth.yml", "gronkh.yml" };
    private static final String ROSTER_FILE = "roster.yml";

    private static volatile BudRegistry instance;

    private final Map<String, BudDefinition> definitions = new ConcurrentHashMap<>();
    private final List<String> defaultBudIds = new ArrayList<>();
    private volatile boolean contentVersionMismatch = false;

    private BudRegistry() {
    }

    public static BudRegistry getInstance() {
        if (instance == null) {
            instance = new BudRegistry();
        }
        return instance;
    }

    public void reloadMissing() {
        this.loadAll(false);
    }

    public void reset() {
        this.loadAll(true);
    }

    private void loadAll(boolean overwriteDefaults) {
        Path rootDataDir = BudPlugin.getInstance().getDataDirectory();
        Path budsDir = Objects.requireNonNull(rootDataDir.resolve("buds"));
        copyPackagedDefaults(budsDir, overwriteDefaults);
        this.contentVersionMismatch = checkContentVersion(rootDataDir, budsDir, overwriteDefaults);
        loadDefinitions(budsDir);
        loadRoster(budsDir.resolve(ROSTER_FILE));
        debugLog();
    }

    private boolean checkContentVersion(@Nonnull Path rootDataDir, @Nonnull Path budsDir, boolean overwriteDefaults) {
        ContentVersion.ensurePackagedCopy(rootDataDir);
        ContentVersion packaged = ContentVersion.loadFromClasspath("/versions.yml");
        ContentVersion runtime = ContentVersion.load(Objects.requireNonNull(rootDataDir.resolve("versions.yml")));
        int packagedVersion = ContentVersion.budVersionOf(packaged);
        int runtimeVersion = ContentVersion.budVersionOf(runtime);
        if (overwriteDefaults) {
            ContentVersion.persistBudVersion(rootDataDir, packagedVersion);
            return false;
        }
        if (runtimeVersion >= packagedVersion) {
            return false;
        }
        if (DebugConfig.getInstance().isAutoUpdateContentOnVersionMismatch()) {
            LoggerUtil.getLogger().info(() -> "[BUD] Bud content outdated (runtime v" + runtimeVersion
                    + ", packaged v" + packagedVersion
                    + ") - AutoUpdateContentOnVersionMismatch is enabled, resetting to packaged defaults.");
            copyPackagedDefaults(budsDir, true, ContentVersion.excludedBudPathsOf(runtime));
            ContentVersion.persistBudVersion(rootDataDir, packagedVersion);
            return false;
        }
        LoggerUtil.getLogger().warning(() -> "[BUD] Bud content outdated (runtime v" + runtimeVersion
                + ", packaged v" + packagedVersion
                + "). Run '/bud reload buds --reset' to update (overwrites your customizations!).");
        return true;
    }

    public boolean isContentVersionMismatch() {
        return this.contentVersionMismatch;
    }

    private void copyPackagedDefaults(Path budsDir, boolean overwrite) {
        copyPackagedDefaults(budsDir, overwrite, Objects.requireNonNull(Set.of()));
    }

    private void copyPackagedDefaults(Path budsDir, boolean overwrite, @Nonnull Set<String> excludedPaths) {
        List<String> resources = new ArrayList<>(List.of(PACKAGED_DEFINITIONS));
        resources.add(ROSTER_FILE);

        for (String res : resources) {
            if (overwrite && excludedPaths.contains(res)) {
                LoggerUtil.getLogger().info(() -> "[BUD] Skipping excluded bud resource: " + res);
                continue;
            }
            Path target = budsDir.resolve(res);
            if (!overwrite && Files.exists(target)) {
                continue;
            }
            try {
                Files.createDirectories(budsDir);
                try (InputStream in = BudPlugin.class.getResourceAsStream("/buds/" + res)) {
                    if (in == null) {
                        LoggerUtil.getLogger()
                                .severe(() -> "[BUD] Default bud resource not found in JAR: /buds/" + res);
                        continue;
                    }
                    if (overwrite) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        LoggerUtil.getLogger().info(() -> "[BUD] Bud resource updated: " + target);
                    } else {
                        Files.copy(in, target);
                        LoggerUtil.getLogger().info(() -> "[BUD] Bud resource created: " + target);
                    }
                }
            } catch (IOException e) {
                LoggerUtil.getLogger()
                        .severe(() -> "[BUD] Failed to copy default bud resource: " + res + " - " + e.getMessage());
            }
        }
    }

    private void loadDefinitions(Path budsDir) {
        definitions.clear();
        if (!Files.isDirectory(budsDir)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Bud definitions directory missing: " + budsDir);
            return;
        }
        try (Stream<Path> files = Files.list(budsDir)) {
            files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase(ROSTER_FILE))
                    .forEach(this::loadDefinitionFile);
        } catch (IOException e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to list bud definitions: " + e.getMessage());
        }
    }

    private void loadDefinitionFile(Path path) {
        try {
            BudDefinition definition = BudDefinition.load(path);
            if (definition == null || definition.getId().isBlank()) {
                LoggerUtil.getLogger().warning(() -> "[BUD] Skipping bud definition without id: " + path);
                return;
            }
            String id = definition.getId();
            if (definitions.containsKey(id)) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Duplicate bud id '" + id + "' in " + path
                                + ", overriding previous definition.");
            }
            definitions.put(id, definition);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to load bud definition " + path + ": " + e.getMessage());
        }
    }

    private void loadRoster(Path rosterFile) {
        defaultBudIds.clear();
        BudRoster roster = null;
        if (Files.exists(rosterFile)) {
            try {
                roster = BudRoster.load(rosterFile);
            } catch (Exception e) {
                LoggerUtil.getLogger()
                        .severe(() -> "[BUD] Failed to load bud roster " + rosterFile + ": " + e.getMessage());
            }
        }
        List<String> configuredIds = roster != null ? roster.getDefaultBuds() : List.of();
        if (configuredIds.size() > 3) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] roster.yml 'defaultBuds' has more than 3 entries (" + configuredIds
                            + ") - PlayerBudComponent only allows 3 simultaneously summoned Buds, extras beyond the "
                            + "3rd valid entry are ignored by the 'create all' shorthand.");
        }
        for (String rawId : configuredIds) {
            String id = normalize(rawId);
            if (!definitions.containsKey(id)) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] roster.yml references unknown bud id '" + id + "', skipping.");
                continue;
            }
            if (defaultBudIds.size() >= 3) {
                continue;
            }
            defaultBudIds.add(id);
        }
    }

    @Nonnull
    public static String normalize(@Nullable String budId) {
        return budId == null ? "" : Objects.requireNonNull(budId.trim().toLowerCase(Locale.ROOT));
    }

    public boolean exists(@Nullable String budId) {
        return definitions.containsKey(normalize(budId));
    }

    @Nonnull
    public BudDefinition get(@Nonnull String budId) {
        BudDefinition definition = definitions.get(normalize(budId));
        if (definition == null) {
            throw new IllegalArgumentException("Unknown Bud id: " + budId);
        }
        return definition;
    }

    @Nonnull
    public Set<String> getIds() {
        return Objects.requireNonNull(Collections.unmodifiableSet(definitions.keySet()));
    }

    @Nonnull
    public List<String> getDefaultBudIds() {
        return Objects.requireNonNull(Collections.unmodifiableList(defaultBudIds));
    }

    public void debugLog() {
        LoggerUtil.getLogger().info(() -> "[BUD] --- BudRegistry Debug ---");
        LoggerUtil.getLogger().finer(() -> "[BUD] Bud definitions loaded: " + definitions.keySet());
        LoggerUtil.getLogger().finer(() -> "[BUD] Default roster: " + defaultBudIds);
        LoggerUtil.getLogger().finer(() -> "[BUD] -------------------------");
    }

}
