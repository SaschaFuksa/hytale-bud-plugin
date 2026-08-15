package com.bud.feature.work;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.types.WorkRole;
import com.hypixel.hytale.server.core.inventory.ItemStack;

final class WorkstationFuelUtil {

    private WorkstationFuelUtil() {
    }

    static boolean isAllowedFuel(@Nullable ItemStack itemStack, @Nonnull WorkRole workRole) {
        if (itemStack == null || itemStack.isEmpty()) {
            return true;
        }
        if (FarmingRecipeConfig.getInstance().getAllowedFuel(workRole).isEmpty()) {
            return true;
        }
        String itemId = Objects.requireNonNull(itemStack.getItem().getId());
        return FarmingRecipeConfig.getInstance().isFuelAllowed(workRole, itemId);
    }

}
