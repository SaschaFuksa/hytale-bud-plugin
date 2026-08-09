package com.bud.core.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.BudPlugin;
import com.bud.llm.messages.AbstractYamlMessage;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Single shared {@code versions.yml} tracking the content version of both the prompt YAMLs
 * ({@code promptVersion}) and the Bud definition YAMLs ({@code budVersion}), compared between
 * the version packaged in the jar and the version copied into the runtime data folder to detect
 * an outdated local copy after a plugin update - see {@code LLMPromptManager}/{@code BudRegistry},
 * which each own their own comparison + reminder log against their relevant field.
 */
public class ContentVersion extends AbstractYamlMessage {

    private int budVersion;
    private int promptVersion;

    public int getBudVersion() {
        return this.budVersion;
    }

    public int getPromptVersion() {
        return this.promptVersion;
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

    public static void ensurePackagedCopy(@Nonnull Path dataDir, boolean overwrite) {
        Path target = dataDir.resolve("versions.yml");
        if (!overwrite && Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(dataDir);
            try (InputStream in = BudPlugin.class.getResourceAsStream("/versions.yml")) {
                if (in == null) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Default versions.yml not found in JAR.");
                    return;
                }
                if (overwrite) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    LoggerUtil.getLogger().info(() -> "[BUD] versions.yml updated: " + target);
                } else {
                    Files.copy(in, target);
                    LoggerUtil.getLogger().info(() -> "[BUD] versions.yml created: " + target);
                }
            }
        } catch (IOException e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to copy versions.yml: " + e.getMessage());
        }
    }

}
