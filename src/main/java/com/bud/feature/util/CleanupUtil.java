package com.bud.feature.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class CleanupUtil {

    public static void cleanupBuds(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
            @Nonnull Set<String> budIds) {
        store.getExternalData().getWorld().execute(() -> {
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null) {
                    return;
                }
                PlayerBudComponent playerBudComponent = store.getComponent(ref,
                        PlayerBudComponent.getComponentType());
                if (playerBudComponent == null) {
                    ChatEvent.dispatch(playerRef, "No Buds found to remove.");
                    return;
                }
                ConcurrentLinkedQueue<NPCEntity> buds = playerBudComponent.getCurrentBuds();
                List<String> removedBuds = new ArrayList<>();
                for (NPCEntity bud : buds) {
                    for (String rawBudId : budIds) {
                        String budId = Objects.requireNonNull(rawBudId);
                        String npcTypeId = BudRegistry.getInstance().get(budId).getNpcTypeId();
                        if (bud.getNPCTypeId().equals(npcTypeId)) {
                            playerBudComponent.removeCurrentBud(bud, budId);
                            Ref<EntityStore> budRef = bud.getReference();
                            if (budRef != null) {
                                despawnBud(budRef, store);
                            }
                            Orchestrator.getInstance().purgeBud(playerRef.getUsername(), budId);
                            removedBuds.add(BudRegistry.getInstance().get(budId).getDisplayName());
                        }
                    }
                }
                String message = removedBuds.isEmpty() ? "No matching Buds found to remove."
                        : "Removed Buds: " + String.join(", ", removedBuds);
                LoggerUtil.getLogger().info(() -> "[BUD] " + message);
                ChatEvent.dispatch(playerRef, message);
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] Exception removing buds: " + e.getMessage());
            }
        });
    }

    public static void cleanupAllBuds(World world, Store<EntityStore> store) {
        ComponentType<EntityStore, NPCEntity> componentType = NPCEntity.getComponentType();
        if (componentType == null) {
            LoggerUtil.getLogger().severe(() -> "[BUD] NPCEntity component type not found.");
            return;
        }
        try {
            ConcurrentLinkedQueue<BudComponent> budsToRemove = new ConcurrentLinkedQueue<>();
            store.forEachEntityParallel(
                    BudComponent.getComponentType(),
                    (index, archetypeChunk, commandBuffer) -> {
                        LoggerUtil.getLogger()
                                .fine(() -> "[BUD] Checking entity for cleanup: " + index);
                        BudComponent budComponent = archetypeChunk.getComponent(index,
                                BudComponent.getComponentType());
                        if (budComponent == null) {
                            return;
                        }
                        budsToRemove.add(budComponent);
                    });
            for (BudComponent budComponent : budsToRemove) {
                NPCEntity bud = budComponent.getBud();
                Ref<EntityStore> ref = bud.getReference();
                if (ref == null) {
                    continue;
                }
                PlayerRef player = budComponent.getPlayerRef();
                Ref<EntityStore> playerRef = player.getReference();
                if (playerRef == null) {
                    continue;
                }
                PlayerBudComponent playerBudComponent = store.getComponent(playerRef,
                        PlayerBudComponent.getComponentType());
                if (playerBudComponent != null) {
                    playerBudComponent.removeCurrentBud(bud, budComponent.getBudId());
                }
                despawnBud(ref, store);
                Orchestrator.getInstance().purgeBud(player.getUsername(), budComponent.getBudId());
                LoggerUtil.getLogger()
                        .info(() -> "[BUD] Removing NPC \"" + bud.getNPCTypeId() + "\""
                                + " for player " + player.getUsername());

            }
            Universe.get().sendMessage(Message.raw("All buds removed."));
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Exception during cleanup of world " + world.getName() + ": " + e.getMessage());
        }
    }

    private static void despawnBud(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        try {
            store.removeEntity(ref, RemoveReason.REMOVE);
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Exception checking entity reference: " + e.getMessage());
        }
    }

}
