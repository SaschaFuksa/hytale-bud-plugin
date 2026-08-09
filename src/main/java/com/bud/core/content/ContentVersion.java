package com.bud.core.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.BudPlugin;
import com.bud.llm.messages.AbstractYamlMessage;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Single shared, operator-editable {@code versions.yml}: tracks the content version of both the
 * prompt YAMLs ({@code promptVersion}) and the Bud definition YAMLs ({@code budVersion}), compared
 * against the version packaged in the jar to detect an outdated runtime copy after a plugin update
 * - see {@code LLMPromptManager}/{@code BudRegistry}, which each own their own comparison + reminder
 * log against their relevant field. Also carries the operator-owned {@code excludedPrompts}/
 * {@code excludedBuds} path lists, skipped when an automatic ({@code AutoUpdateContentOnVersionMismatch})
 * sync would otherwise overwrite them - an explicit {@code --reset} command still ignores these.
 */
public class ContentVersion extends AbstractYamlMessage {

    private int budVersion;
    private int promptVersion;
    private List<String> excludedPrompts;
    private List<String> excludedBuds;

    public int getBudVersion() {
        return this.budVersion;
    }

    public int getPromptVersion() {
        return this.promptVersion;
    }

    @Nonnull
    public Set<String> getExcludedPromptPaths() {
        return Objects.requireNonNull(excludedPrompts != null ? Set.copyOf(excludedPrompts) : Set.of());
    }

    @Nonnull
    public Set<String> getExcludedBudPaths() {
        return Objects.requireNonNull(excludedBuds != null ? Set.copyOf(excludedBuds) : Set.of());
    }

    @Nullable
    public static ContentVersion load(@Nonnull Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        return loadFromFile(ContentVersion.class, path);
    }

    @Nullable
    public static ContentVersion loadFromClasspath(@Nonnull String resourcePath) {
        try (InputStream in = BudPlugin.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LoggerUtil.getLogger()
                        .severe(() -> "[BUD] Packaged content version resource not found in JAR: " + resourcePath);
                return null;
            }
            return loadFromStream(ContentVersion.class, in);
        } catch (IOException e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Failed to read packaged content version " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    public static int budVersionOf(@Nullable ContentVersion contentVersion) {
        return contentVersion != null ? contentVersion.getBudVersion() : 0;
    }

    public static int promptVersionOf(@Nullable ContentVersion contentVersion) {
        return contentVersion != null ? contentVersion.getPromptVersion() : 0;
    }

    @Nonnull
    public static Set<String> excludedPromptPathsOf(@Nullable ContentVersion contentVersion) {
        return Objects.requireNonNull(
                contentVersion != null ? contentVersion.getExcludedPromptPaths() : Set.of());
    }

    @Nonnull
    public static Set<String> excludedBudPathsOf(@Nullable ContentVersion contentVersion) {
        return Objects.requireNonNull(
                contentVersion != null ? contentVersion.getExcludedBudPaths() : Set.of());
    }

    public static void ensurePackagedCopy(@Nonnull Path rootDataDir) {
        Path target = rootDataDir.resolve("versions.yml");
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(rootDataDir);
            try (InputStream in = BudPlugin.class.getResourceAsStream("/versions.yml")) {
                if (in == null) {
                    LoggerUtil.getLogger().severe("[BUD] Default versions.yml not found in JAR.");
                    return;
                }
                Files.copy(in, target);
                LoggerUtil.getLogger().info(() -> "[BUD] versions.yml created: " + target);
            }
        } catch (IOException e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to create versions.yml: " + e.getMessage());
        }
    }

    public static void persistBudVersion(@Nonnull Path rootDataDir, int newBudVersion) {
        ContentVersion current = load(Objects.requireNonNull(rootDataDir.resolve("versions.yml")));
        write(rootDataDir, newBudVersion, promptVersionOf(current),
                excludedPromptPathsOf(current), excludedBudPathsOf(current));
    }

    public static void persistPromptVersion(@Nonnull Path rootDataDir, int newPromptVersion) {
        ContentVersion current = load(Objects.requireNonNull(rootDataDir.resolve("versions.yml")));
        write(rootDataDir, budVersionOf(current), newPromptVersion,
                excludedPromptPathsOf(current), excludedBudPathsOf(current));
    }

    private static void write(@Nonnull Path rootDataDir, int budVersion, int promptVersion,
            @Nonnull Set<String> excludedPrompts, @Nonnull Set<String> excludedBuds) {
        Path target = rootDataDir.resolve("versions.yml");
        StringBuilder sb = new StringBuilder();
        sb.append("budVersion: ").append(budVersion).append('\n');
        sb.append("promptVersion: ").append(promptVersion).append('\n');
        appendPathList(sb, "excludedPrompts", excludedPrompts);
        appendPathList(sb, "excludedBuds", excludedBuds);
        try {
            Files.createDirectories(rootDataDir);
            Files.writeString(target, sb.toString());
            LoggerUtil.getLogger().info(() -> "[BUD] versions.yml updated: " + target);
        } catch (IOException e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to persist versions.yml: " + e.getMessage());
        }
    }

    private static void appendPathList(@Nonnull StringBuilder sb, @Nonnull String key, @Nonnull Set<String> paths) {
        if (paths.isEmpty()) {
            return;
        }
        sb.append(key).append(":\n");
        for (String path : paths) {
            sb.append("  - \"").append(path.replace("\"", "\\\"")).append("\"\n");
        }
    }

}
