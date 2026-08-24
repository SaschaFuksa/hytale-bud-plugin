package com.bud.feature.work;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class WorldBlockEntities {

    private WorldBlockEntities() {
    }

    @Nullable
    public static Ref<ChunkStore> ensureOrFetch(@Nonnull WorldChunk chunk, int x, int y, int z) {
        try {
            return BlockModule.ensureBlockEntity(chunk, x, y, z);
        } catch (IllegalArgumentException e) {
            return chunk.getBlockComponentEntity(x, y, z);
        }
    }

}
