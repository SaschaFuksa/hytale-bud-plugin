package com.bud.feature.work;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class BlockEntityPositions {

    private BlockEntityPositions() {
    }

    @Nullable
    public static Vector3i resolve(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref) {
        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref,
                Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()));
        if (blockStateInfo == null) {
            return null;
        }
        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        if (!chunkRef.isValid()) {
            return null;
        }
        BlockChunk blockChunk = chunkStore.getComponent(chunkRef,
                Objects.requireNonNull(BlockChunk.getComponentType()));
        if (blockChunk == null) {
            return null;
        }
        int index = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int localY = ChunkUtil.yFromBlockInColumn(index);
        int localZ = ChunkUtil.zFromBlockInColumn(index);
        int worldX = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getX(), localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getZ(), localZ);
        return new Vector3i(worldX, localY, worldZ);
    }

}
