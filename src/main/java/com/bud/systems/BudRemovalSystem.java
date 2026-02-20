package com.bud.systems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.cleanup.CleanUpHandler;
import com.bud.components.BudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.EntityChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BudRemovalSystem extends RefSystem<EntityStore> {

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.and(BudComponent.getComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason addReason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason removeReason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.getExternalData().getWorld().execute(() -> {
            LoggerUtil.getLogger().info(() -> "[BUD] NPC removed: " + ref.toString());
            CleanUpHandler.cleanupAllBuds(store.getExternalData().getWorld());
            CleanUpHandler.cleanupAllBuds(ref.getStore().getExternalData().getWorld());
            LoggerUtil.getLogger().info(() -> "[BUD] NPC removed: Blab");
            TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());

            assert transformComponent != null;

            Ref<ChunkStore> chunkRef = transformComponent.getChunkRef();
            if (chunkRef != null && chunkRef.isValid()) {
                World world = commandBuffer.getExternalData().getWorld();
                ChunkStore chunkStore = world.getChunkStore();
                Store<ChunkStore> chunkComponentStore = chunkStore.getStore();
                EntityChunk entityChunkComponent = chunkComponentStore.getComponent(chunkRef,
                        EntityChunk.getComponentType());

                assert entityChunkComponent != null;

                switch (removeReason) {
                    case REMOVE:
                        entityChunkComponent.removeEntityReference(ref);
                        break;
                    case UNLOAD:
                        entityChunkComponent.unloadEntityReference(ref);
                }
            }
        });
    }

}
