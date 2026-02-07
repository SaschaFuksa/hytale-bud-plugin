package com.bud.cleanup;

import com.bud.npc.NPCManager;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class CleanupSystem extends RefSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (reason == AddReason.LOAD) {
            ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
            if (npcType == null) {
                return;
            }

            NPCEntity npc = store.getComponent(ref, npcType);
            if (npc != null) {
                Set<String> trackedTypes = NPCManager.getInstance().getTrackedBudTypes();
                if (trackedTypes.contains(npc.getNPCTypeId())) {
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Cleaning up orphan Bud loaded from disk: " + npc.getUuid());
                    commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
                }
            }
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Nothing to do
    }
}
