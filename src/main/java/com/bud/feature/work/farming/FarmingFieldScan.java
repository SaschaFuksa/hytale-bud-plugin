package com.bud.feature.work.farming;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.bud.feature.work.FieldCandidates;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.HarvestingDropType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;

public final class FarmingFieldScan {

    private FarmingFieldScan() {
    }

    public static boolean isHarvestCandidate(@Nonnull World world, @Nonnull Vector3i position) {
        if (!FieldCandidates.isTilledSoil(FieldCandidates.getBlockType(world, position.x, position.y, position.z))) {
            return false;
        }
        BlockType above = FieldCandidates.getBlockType(world, position.x, position.y + 1, position.z);
        if (above == null || above == BlockType.EMPTY) {
            return false;
        }
        BlockGathering gathering = above.getGathering();
        return gathering != null && gathering.isHarvestable();
    }

    public static boolean hasHarvestOutputRoom(@Nonnull World world, @Nonnull Vector3i position,
            @Nonnull ProcessingBenchBlock processingBenchBlock) {
        ItemContainer output = processingBenchBlock.getOutputContainer();
        if (output == null) {
            return false;
        }
        String itemId = resolveHarvestItemId(world, position);
        if (itemId == null) {
            return true;
        }
        return output.canAddItemStacks(List.of(new ItemStack(itemId, 1)), false, false);
    }

    @Nullable
    public static String resolveHarvestItemId(@Nonnull World world, @Nonnull Vector3i position) {
        BlockType above = FieldCandidates.getBlockType(world, position.x, position.y + 1, position.z);
        BlockGathering gathering = above != null ? above.getGathering() : null;
        HarvestingDropType harvest = gathering != null ? gathering.getHarvest() : null;
        return harvest != null ? harvest.getItemId() : null;
    }

}
