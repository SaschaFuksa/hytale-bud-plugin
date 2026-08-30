package com.bud.feature.work;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public final class WorkstationLookup {

    private WorkstationLookup() {
    }

    @Nullable
    public static WorkstationBlockEntity resolveLive(@Nonnull World world, int x, int y, int z) {
        Ref<ChunkStore> ref = BlockModule.getBlockEntity(world, x, y, z);
        if (ref == null || !ref.isValid()) {
            return null;
        }
        return world.getChunkStore().getStore().getComponent(ref, WorkstationBlockEntity.getComponentType());
    }

}
