package com.bud.system;

import java.util.UUID;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

import com.bud.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudDamageFilterSystem extends DamageEventSystem {

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
            Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) {
                return; // Not entity-to-entity damage
            }

            Ref<EntityStore> attackerRef = entitySource.getRef();

            // Resolve Entities
            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            NPCEntity targetNPC = store.getComponent(targetRef, NPCEntity.getComponentType());

            // Case 1: Player attacks NPC
            if (attackerPlayer != null && targetNPC != null && playerHasBud(attackerPlayer.getUuid())) {
                RecentOpponentCache.addOpponent(
                        attackerPlayer.getUuid(),
                        targetNPC.getRoleName(),
                        RecentOpponentCache.CombatState.ATTACKED);
                LoggerUtil.getLogger().finer(() -> "[BUD] Damage Event: " + attackerPlayer.getUuid() + " attacked NPC "
                        + targetNPC.getRoleName());
                return;
            }

            NPCEntity attackerNPC = store.getComponent(attackerRef, NPCEntity.getComponentType());
            Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());

            // Case 2: NPC attacks Player
            if (attackerNPC != null && targetPlayer != null && playerHasBud(targetPlayer.getUuid())) {
                RecentOpponentCache.addOpponent(
                        targetPlayer.getUuid(),
                        attackerNPC.getRoleName(),
                        RecentOpponentCache.CombatState.WAS_ATTACKED);
                LoggerUtil.getLogger()
                        .finer(() -> "[BUD] Damage Event: " + attackerNPC.getRoleName() + " attacked player "
                                + targetPlayer.getUuid());
            }

        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Error in BudDamageFilterSystem: " + e.getMessage());
        }
    }

    private boolean playerHasBud(UUID playerId) {
        return !BudRegistry.getInstance().getByOwner(playerId).isEmpty();
    }
}
