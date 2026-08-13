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

    private static final String CROP_PREFIX = "Plant_Crop_";

    private static final String CROP_SUFFIX = "_Block";

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
        if (!FarmingRecipeConfig.getInstance().isSeedAllowed(workRole, seedItemId)) {
            return null;
        }
        String cropBlockType = deriveCropBlockType(seedItemId);
        if (cropBlockType == null && LOGGED_UNRESOLVED_SEEDS.add(seedItemId)) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Seedbag holds " + seedItemId
                    + " but no matching crop block could be resolved/verified for it - PLANT stays skipped"
                    + " for this seed, other work types are unaffected.");
        }
        return cropBlockType;
    }

    static boolean isAllowedOrNotASeed(@Nullable ItemStack itemStack, @Nonnull WorkRole workRole) {
        if (itemStack == null || itemStack.isEmpty()) {
            return true;
        }
        String itemId = Objects.requireNonNull(itemStack.getItem().getId());
        if (!itemId.startsWith(SEED_PREFIX)) {
            return true;
        }
        return FarmingRecipeConfig.getInstance().isSeedAllowed(workRole, itemId);
    }

    @Nullable
    private static String deriveCropBlockType(@Nonnull String seedItemId) {
        if (!seedItemId.startsWith(SEED_PREFIX)) {
            return null;
        }
        String rest = seedItemId.substring(SEED_PREFIX.length());
        boolean eternal = rest.endsWith(ETERNAL_SUFFIX);
        String variety = eternal ? rest.substring(0, rest.length() - ETERNAL_SUFFIX.length()) : rest;
        if (variety.isEmpty()) {
            return null;
        }
        String candidate = CROP_PREFIX + variety + CROP_SUFFIX + (eternal ? ETERNAL_SUFFIX : "");
        return BlockType.fromString(candidate) != null ? candidate : null;
    }

}
