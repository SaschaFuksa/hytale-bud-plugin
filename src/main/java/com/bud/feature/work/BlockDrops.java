package com.bud.feature.work;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;

public final class BlockDrops {

    private BlockDrops() {
    }

    @Nonnull
    public static List<ItemStack> resolveBreakingDrops(@Nullable BlockType blockType) {
        if (blockType == null) {
            return Objects.requireNonNull(List.of());
        }
        BlockGathering gathering = blockType.getGathering();
        BlockBreakingDropType breaking = gathering != null ? gathering.getBreaking() : null;
        if (breaking == null) {
            return Objects.requireNonNull(List.of());
        }
        return BlockHarvestUtils.getDrops(blockType, breaking.getQuantity(), breaking.getItemId(),
                breaking.getDropListId());
    }

}
