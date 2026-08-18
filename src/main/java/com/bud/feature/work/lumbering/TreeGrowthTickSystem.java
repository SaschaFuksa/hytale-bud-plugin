package com.bud.feature.work.lumbering;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.bud.feature.work.BlockEntityPositions;
import com.bud.feature.work.FieldCandidates;
import com.bud.feature.work.WorkRecipeConfig;
import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.Rangef;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class TreeGrowthTickSystem extends EntityTickingSystem<ChunkStore> {

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(Objects.requireNonNull(FarmingBlock.getComponentType()));
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        WorkRecipeConfig config = WorkRecipeConfig.getInstance();
        if (!config.hasTreeGrowthOverrides()) {
            return;
        }
        FarmingBlock farming = archetypeChunk.getComponent(index, FarmingBlock.getComponentType());
        if (farming == null) {
            return;
        }
        Integer overrideSeconds = config.getTreeGrowthStageSeconds(farming.getGeneration());
        if (overrideSeconds == null || farming.getGrowthProgress() < overrideSeconds.floatValue()) {
            return;
        }
        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        Vector3i position = BlockEntityPositions.resolve(store, ref);
        if (position == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        BlockType blockType = FieldCandidates.getBlockType(world, position.x, position.y, position.z);
        if (blockType == null || !isSapling(blockType)) {
            return;
        }
        Float stageDuration = resolveStageDuration(blockType, farming);
        if (stageDuration == null || farming.getGrowthProgress() >= stageDuration.floatValue()) {
            return;
        }
        farming.setGrowthProgress(stageDuration.floatValue());
    }

    private static boolean isSapling(@Nullable BlockType blockType) {
        String blockId = FieldCandidates.getBlockId(blockType);
        return blockId != null && WorkRecipeConfig.getInstance().isSaplingBlock(blockId);
    }

    @Nullable
    private static Float resolveStageDuration(@Nonnull BlockType blockType, @Nonnull FarmingBlock farming) {
        FarmingData farmingData = blockType.getFarming();
        if (farmingData == null) {
            return null;
        }
        Map<String, FarmingStageData[]> allStages = farmingData.getStages();
        if (allStages == null) {
            return null;
        }
        String stageSet = farming.getCurrentStageSet();
        FarmingStageData[] stages = allStages.get(stageSet != null ? stageSet : farmingData.getStartingStageSet());
        if (stages == null) {
            return null;
        }
        int stage = farming.getGeneration();
        if (stage < 0 || stage >= stages.length) {
            return null;
        }
        Rangef duration = stages[stage].getDuration();
        return duration != null ? Float.valueOf(duration.max) : null;
    }

}
