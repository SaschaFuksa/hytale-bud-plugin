package com.bud.feature.work.mining;

import javax.annotation.Nonnull;

import org.joml.Vector3i;

import com.bud.feature.work.BlockEntityPositions;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class OreGrowthTickSystem extends EntityTickingSystem<ChunkStore> {

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(OreGrowthBlock.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        OreGrowthBlock growth = archetypeChunk.getComponent(index, OreGrowthBlock.getComponentType());
        if (growth == null) {
            return;
        }
        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        Vector3i position = BlockEntityPositions.resolve(store, ref);
        if (position == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        MiningGrowthUtil.advanceIfDue(world, position, growth);
    }

}
