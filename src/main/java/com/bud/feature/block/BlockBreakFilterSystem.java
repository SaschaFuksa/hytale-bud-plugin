package com.bud.feature.block;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.feature.item.ItemUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockBreakFilterSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public BlockBreakFilterSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull BreakBlockEvent event) {
        try {
            Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);

            Player player = store.getComponent(entityRef, Player.getComponentType());

            if (player != null) {
                PlayerBudComponent playerBudComponent = store.getComponent(entityRef,
                        PlayerBudComponent.getComponentType());
                BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
                if (budComponent != null) {
                    String blockId = event.getBlockType().getId();
                    if (blockId.contains("Empty")) {
                        return;
                    }

                    final String blockName = ItemUtil.getDisplayName(blockId);

                    LoggerUtil.getLogger().finer(() -> "[BUD] Block Break Event: " +
                            player.getDisplayName() + " broke "
                            + blockName);
                    RecentBlockCache.getInstance().add(player.getDisplayName(), new BlockEntry(blockName,
                            BlockInteraction.BREAK, budComponent));
                }
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in BlockBreakFilterSystem: " + e.getMessage());
        }
    }

}
