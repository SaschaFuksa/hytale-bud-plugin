package com.bud.feature.work;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class WorldBlockSections {

    private static final int MAX_WORLD_HEIGHT = 320;

    private WorldBlockSections() {
    }

    public static boolean setTicking(@Nonnull World world, int x, int y, int z, boolean ticking) {
        Ref<ChunkStore> sectionRef = sectionRefAt(world, x, y, z);
        if (sectionRef == null) {
            return false;
        }
        Store<ChunkStore> store = world.getChunkStore().getStore();
        BlockSection blockSection = store.getComponent(sectionRef,
                Objects.requireNonNull(BlockSection.getComponentType()));
        if (blockSection == null || !blockSection.setTicking(x, y, z, ticking)) {
            return false;
        }
        ChunkSection chunkSection = store.getComponent(sectionRef,
                Objects.requireNonNull(ChunkSection.getComponentType()));
        if (chunkSection != null) {
            chunkSection.markNeedsSaving();
        }
        return true;
    }

    public static int getRotationIndex(@Nonnull World world, int x, int y, int z) {
        Ref<ChunkStore> sectionRef = sectionRefAt(world, x, y, z);
        if (sectionRef == null) {
            return 0;
        }
        BlockSection blockSection = world.getChunkStore().getStore().getComponent(sectionRef,
                Objects.requireNonNull(BlockSection.getComponentType()));
        if (blockSection == null) {
            return 0;
        }
        return blockSection.getRotationIndex(x, y, z);
    }

    @Nullable
    private static Ref<ChunkStore> sectionRefAt(@Nonnull World world, int x, int y, int z) {
        if (y < 0 || y >= MAX_WORLD_HEIGHT) {
            return null;
        }
        Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);
        if (sectionRef == null || !sectionRef.isValid()) {
            return null;
        }
        return sectionRef;
    }

}
