package com.bud.feature.combat;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

public class DamageFilterSystem extends DamageEventSystem {

    private static final double ASSIST_ATTITUDE_DURATION = 3000.0;

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage) {
        try {
            ComponentType<EntityStore, NPCEntity> npcComponentType = NPCEntity.getComponentType();
            if (npcComponentType == null) {
                LoggerUtil.getLogger().severe(() -> "[BUD] NPCEntity component type is not available.");
                return;
            }
            Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) {
                return; // Not entity-to-entity damage
            }

            Ref<EntityStore> attackerRef = entitySource.getRef();

            NPCEntity targetNPC = store.getComponent(targetRef, npcComponentType);
            Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());

            if (targetNPC != null) {
                handle(store, attackerRef, targetNPC, CombatState.ATTACKED);
            }

            NPCEntity attackerNPC = store.getComponent(attackerRef, npcComponentType);

            if (attackerNPC != null && targetPlayer != null) {
                Ref<EntityStore> playerRef = targetPlayer.getReference();
                if (playerRef == null) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Target player reference is null.");
                    return;
                }
                if (BudManager.getInstance().findBudComponent(attackerNPC) != null) {
                    damage.setCancelled(true);
                }
                handle(store, playerRef, attackerNPC, CombatState.WAS_ATTACKED);
            }

        } catch (

        Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in BudDamageFilterSystem: " + e.getMessage());
        }
    }

    private void handle(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> playerRef, @Nonnull NPCEntity npc,
            @Nonnull CombatState state) {
        PlayerBudComponent playerBudComponent = store.getComponent(playerRef,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            return;
        }

        if (BudManager.getInstance().findBudComponent(npc) == null) {
            Ref<EntityStore> opponentRef = npc.getReference();
            for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
                Role budRole = bud.getRole();
                if (budRole != null) {
                    try {
                        budRole.getWorldSupport().overrideAttitude(opponentRef, Attitude.HOSTILE,
                                ASSIST_ATTITUDE_DURATION);
                    } catch (NullPointerException e) {
                        LoggerUtil.getLogger().warning(() -> "[BUD] Could not override attitude for "
                                + bud.getRoleName()
                                + " - its Role has no attitude-override memory allocated (needs an OverrideAttitude action in its Instructions, and a server/world restart to rebuild already-spawned NPCs).");
                    }
                }
            }
        }

        BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
        if (budComponent == null) {
            return;
        }
        String entityName = npc.getRoleName() != null ? npc.getRoleName() : "Unknown NPC";
        if (entityName == null) {
            return;
        }
        RecentOpponentCache.getInstance().add(
                playerBudComponent.getPlayerRef().getUsername(),
                new OpponentEntry(
                        entityName,
                        state,
                        budComponent));
        LoggerUtil.getLogger()
                .finer(() -> "[BUD] Damage Event: " + playerBudComponent.getPlayerRef().getUsername() + " with state "
                        + state + " by NPC "
                        + entityName);
    }

}
