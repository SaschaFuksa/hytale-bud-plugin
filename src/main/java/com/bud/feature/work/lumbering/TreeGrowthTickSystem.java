package com.bud.feature.work.lumbering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.feature.work.WorkRecipeConfig;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.Rangef;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingData;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.farming.FarmingStageData;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class TreeGrowthTickSystem extends EntityTickingSystem<ChunkStore> {

    @Nonnull
    private static final Map<Rangef, float[]> VANILLA_DURATIONS = Objects
            .requireNonNull(Collections.synchronizedMap(new IdentityHashMap<>()));

    @Nonnull
    private static final AtomicBoolean APPLIED = Objects.requireNonNull(new AtomicBoolean());

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(Objects.requireNonNull(FarmingBlock.getComponentType()));
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        if (APPLIED.get()) {
            return;
        }
        WorkRecipeConfig config = WorkRecipeConfig.getInstance();
        if (!config.hasTreeGrowthOverrides() || !APPLIED.compareAndSet(false, true)) {
            return;
        }
        applyOverrides(config);
    }

    private static void applyOverrides(@Nonnull WorkRecipeConfig config) {
        List<BlockType> saplings = collectSaplingBlockTypes();
        Set<FarmingStageData> patchedStageData = Objects
                .requireNonNull(Collections.newSetFromMap(new IdentityHashMap<>()));
        int patchedDurations = 0;
        for (BlockType blockType : saplings) {
            patchedDurations += patchBlockType(Objects.requireNonNull(blockType), patchedStageData, config);
        }
        int blockTypeCount = saplings.size();
        int durationCount = patchedDurations;
        if (blockTypeCount == 0) {
            LoggerUtil.getLogger().warning(() -> "[BUD] 'treeGrowthStageSeconds' is configured but no sapling block "
                    + "type matched the LUMBERING seedTargetPattern prefix - tree growth stays at vanilla speed.");
            return;
        }
        LoggerUtil.getLogger().info(() -> "[BUD] Tree growth override applied: " + durationCount
                + " stage durations on " + blockTypeCount + " sapling block types.");
    }

    @Nonnull
    private static List<BlockType> collectSaplingBlockTypes() {
        BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
        if (assetMap == null) {
            return Objects.requireNonNull(List.of());
        }
        Set<BlockType> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<BlockType> saplings = new ArrayList<>();
        Map<String, BlockType> byKey = assetMap.getAssetMap();
        if (byKey != null) {
            for (BlockType blockType : byKey.values()) {
                if (isSapling(blockType) && seen.add(blockType)) {
                    saplings.add(blockType);
                }
            }
        }
        int assetCount = assetMap.getNextIndex();
        for (int assetIndex = 0; assetIndex < assetCount; assetIndex++) {
            BlockType blockType = assetMap.getAsset(assetIndex);
            if (isSapling(blockType) && seen.add(blockType)) {
                saplings.add(blockType);
            }
        }
        return Objects.requireNonNull(saplings);
    }

    private static int patchBlockType(@Nonnull BlockType blockType,
            @Nonnull Set<FarmingStageData> patchedStageData, @Nonnull WorkRecipeConfig config) {
        FarmingData farmingData = blockType.getFarming();
        if (farmingData == null) {
            return 0;
        }
        Map<String, FarmingStageData[]> allStages = farmingData.getStages();
        if (allStages == null) {
            return 0;
        }
        int patched = 0;
        for (FarmingStageData[] stages : allStages.values()) {
            if (stages == null) {
                continue;
            }
            for (int stage = 0; stage < stages.length; stage++) {
                FarmingStageData stageData = stages[stage];
                if (stageData == null || !patchedStageData.add(stageData)) {
                    continue;
                }
                if (applyStageDuration(stageData, stage, config)) {
                    patched++;
                }
            }
        }
        return patched;
    }

    private static boolean applyStageDuration(@Nonnull FarmingStageData stageData, int stage,
            @Nonnull WorkRecipeConfig config) {
        Rangef duration = stageData.getDuration();
        if (duration == null) {
            return false;
        }
        float[] vanilla = VANILLA_DURATIONS.computeIfAbsent(duration,
                range -> new float[] { range.min, range.max });
        Integer overrideSeconds = config.getTreeGrowthStageSeconds(stage);
        if (overrideSeconds == null) {
            duration.min = vanilla[0];
            duration.max = vanilla[1];
            return false;
        }
        duration.min = overrideSeconds.floatValue();
        duration.max = overrideSeconds.floatValue();
        return true;
    }

    private static boolean isSapling(@Nullable BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        String blockId = blockType.getId();
        return blockId != null && blockType.getFarming() != null
                && WorkRecipeConfig.getInstance().isSaplingBlock(blockId);
    }

}
