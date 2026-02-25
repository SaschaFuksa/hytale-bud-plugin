package com.bud.feature.discover;

import javax.annotation.Nonnull;

import com.bud.feature.data.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Filter system for DiscoverZoneEvent.Display.
 * Captures when players discover new zones to provide context for Buds.
 */
public class DiscoverZoneFilterSystem extends EntityEventSystem<EntityStore, DiscoverZoneEvent.Display> {

    public DiscoverZoneFilterSystem() {
        super(DiscoverZoneEvent.Display.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull DiscoverZoneEvent.Display event) {
        try {
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
            Player player = store.getComponent(entityRef, Player.getComponentType());

            if (player != null) {
                UUID playerId = player.getUuid();
                if (BudRegistry.playerHasBud(playerId)) {
                    var info = event.getDiscoveryInfo();
                    String zoneName = info.zoneName();
                    String regionName = info.regionName();
                    boolean major = info.major();

                    LoggerUtil.getLogger().finer(() -> "[BUD] Discover Zone Event: " + player.getDisplayName()
                            + " discovered zone=" + zoneName + " region=" + regionName + " major=" + major);

                    RecentDiscoverCache.getInstance().add(playerId,
                            new DiscoverEntry(zoneName, regionName, major));
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in DiscoverZoneFilterSystem: " + e.getMessage());
        }
    }
}
