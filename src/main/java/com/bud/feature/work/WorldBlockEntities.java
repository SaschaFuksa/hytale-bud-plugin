package com.bud.feature.work;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class WorldBlockEntities {

    private WorldBlockEntities() {
    }

    @Nullable
    public static Ref<ChunkStore> ensureOrFetch(@Nonnull WorldChunk chunk, int x, int y, int z) {
        Ref<ChunkStore> existing = chunk.getBlockComponentEntity(x, y, z);
        if (existing != null) {
            return existing;
        }

        World world = chunk.getWorld();
        if (world.isInThread()) {
            return spawnBlockEntity(chunk, x, y, z);
        }
        CompletableFuture<Ref<ChunkStore>> future = new CompletableFuture<>();
        world.execute(() -> future.complete(spawnBlockEntity(chunk, x, y, z)));
        return future.join();
    }

    @Nullable
    private static Ref<ChunkStore> spawnBlockEntity(@Nonnull WorldChunk chunk, int x, int y, int z) {
        BlockType blockType = chunk.getBlockType(x, y, z);
        if (blockType == null) {
            return null;
        }
        Holder<ChunkStore> template = blockType.getBlockEntity();
        if (template == null) {
            return null;
        }

        Holder<ChunkStore> data = template.clone();
        data.putComponent(Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()),
                new BlockModule.BlockStateInfo(ChunkUtil.indexBlockInColumn(x, y, z),
                        Objects.requireNonNull(chunk.getReference())));
        return chunk.getWorld().getChunkStore().getStore().addEntity(data, AddReason.SPAWN);
    }

}
