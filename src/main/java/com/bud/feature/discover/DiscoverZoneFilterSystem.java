package com.bud.feature.discover;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DiscoverZoneEvent;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker.ZoneDiscoveryInfo;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
                PlayerBudComponent playerBudComponent = store.getComponent(entityRef,
                        PlayerBudComponent.getComponentType());
                BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
                if (budComponent != null) {
                    ZoneDiscoveryInfo info = event.getDiscoveryInfo();
                    String zoneName = info.zoneName();
                    String regionName = info.regionName();
                    boolean major = info.major();

                    LoggerUtil.getLogger().finer(() -> "[BUD] Discover Zone Event: " + player.getDisplayName()
                            + " discovered zone=" + zoneName + " region=" + regionName + " major=" + major);

                    RecentDiscoverCache.getInstance().add(player.getDisplayName(),
                            new DiscoverEntry(zoneName, regionName, major, budComponent));
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in DiscoverZoneFilterSystem: " + e.getMessage());
        }
    }
}
