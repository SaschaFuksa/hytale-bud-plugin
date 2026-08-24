package com.bud.feature.work;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;

public final class WorkstationSeedUtil {

    public static final short SEEDBAG_SLOT = 1;

    private static final String SEED_PREFIX = "Plant_Seeds_";

    private static final String ETERNAL_SUFFIX = "_Eternal";

    @Nonnull
    private static final Set<String> LOGGED_UNRESOLVED_SEEDS = Objects
            .requireNonNull(Collections.synchronizedSet(new HashSet<>()));

    private WorkstationSeedUtil() {
    }

    @Nullable
    public static String resolveCropBlockType(@Nullable ItemStack seedStack, @Nonnull WorkRole workRole) {
        if (seedStack == null || seedStack.isEmpty()) {
            return null;
        }
        String seedItemId = Objects.requireNonNull(seedStack.getItem().getId());
        if (!WorkRecipeConfig.getInstance().isSeedAllowed(workRole, seedItemId)) {
            return null;
        }
        String cropBlockType = deriveCropBlockType(seedItemId, workRole);
        if (cropBlockType == null && LOGGED_UNRESOLVED_SEEDS.add(seedItemId)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Seedbag holds " + seedItemId
                    + " but no matching crop block could be resolved/verified for it - PLANT stays skipped"
                    + " for this seed, other work types are unaffected.");
        }
        return cropBlockType;
    }

    static boolean isAllowedSeed(@Nullable ItemStack itemStack, @Nonnull WorkRole workRole) {
        if (itemStack == null || itemStack.isEmpty()) {
            return true;
        }
        String itemId = Objects.requireNonNull(itemStack.getItem().getId());
        return WorkRecipeConfig.getInstance().isSeedAllowed(workRole, itemId);
    }

    @Nullable
    private static String deriveCropBlockType(@Nonnull String seedItemId, @Nonnull WorkRole workRole) {
        if (!seedItemId.startsWith(SEED_PREFIX)) {
            return null;
        }
        String override = WorkRecipeConfig.getInstance().getSeedTargetOverride(seedItemId);
        if (override != null) {
            return BlockType.fromString(override) != null ? override : null;
        }
        WorkRecipeConfig.SeedTargetPattern pattern = WorkRecipeConfig.getInstance()
                .getSeedTargetPattern(workRole);
        if (pattern == null) {
            if (LOGGED_UNRESOLVED_SEEDS.add(seedItemId)) {
                LoggerUtil.getLogger().warning(() -> "[BUD] No 'seedTargetPattern' configured for WorkRole "
                        + workRole + " - PLANT stays skipped for " + seedItemId + ".");
            }
            return null;
        }
        String rest = seedItemId.substring(SEED_PREFIX.length());
        boolean eternal = rest.endsWith(ETERNAL_SUFFIX);
        String variety = eternal ? rest.substring(0, rest.length() - ETERNAL_SUFFIX.length()) : rest;
        if (variety.isEmpty()) {
            return null;
        }
        String candidate = pattern.prefix() + variety + pattern.suffix() + (eternal ? ETERNAL_SUFFIX : "");
        return BlockType.fromString(candidate) != null ? candidate : null;
    }

}
