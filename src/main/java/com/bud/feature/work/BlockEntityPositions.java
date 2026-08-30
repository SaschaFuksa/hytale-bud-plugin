package com.bud.feature.work;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
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
        Vector3i position = new Vector3i();
        if (!blockStateInfo.fillWorldPos(chunkStore, position)) {
            return null;
        }
        return position;
    }

}
