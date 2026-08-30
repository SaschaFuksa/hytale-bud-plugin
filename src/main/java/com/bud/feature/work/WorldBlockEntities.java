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
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class WorldBlockEntities {

    private WorldBlockEntities() {
    }

    @Nullable
    public static Ref<ChunkStore> ensureOrFetch(@Nonnull World world, int x, int y, int z) {
        Ref<ChunkStore> existing = BlockModule.getBlockEntity(world, x, y, z);
        if (existing != null) {
            return existing;
        }

        if (world.isInThread()) {
            return spawnBlockEntity(world, x, y, z);
        }
        CompletableFuture<Ref<ChunkStore>> future = new CompletableFuture<>();
        world.execute(() -> future.complete(spawnBlockEntity(world, x, y, z)));
        return future.join();
    }

    @Nullable
    private static Ref<ChunkStore> spawnBlockEntity(@Nonnull World world, int x, int y, int z) {
        BlockType blockType = world.getBlockType(x, y, z);
        if (blockType == null) {
            return null;
        }
        Holder<ChunkStore> template = blockType.getBlockEntity();
        if (template == null) {
            return null;
        }
        ChunkStore chunkStore = world.getChunkStore();
        Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return null;
        }

        Holder<ChunkStore> data = template.clone();
        data.putComponent(Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()),
                new BlockModule.BlockStateInfo(ChunkUtil.indexBlock(x, y, z), sectionRef));
        return chunkStore.getStore().addEntity(data, AddReason.SPAWN);
    }

}
