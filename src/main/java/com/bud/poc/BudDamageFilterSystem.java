package com.bud.poc;

import com.bud.system.RecentOpponentCache;
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
            if (attackerPlayer != null && targetNPC != null) {
                RecentOpponentCache.addOpponent(
                        attackerPlayer.getUuid(),
                        targetNPC.getRoleName(),
                        RecentOpponentCache.CombatState.ATTACKED);
                System.err.println("[BUD] Damage Event: " + attackerPlayer.getUuid() + " attacked NPC "
                        + targetNPC.getRoleName());
                return;
            }

            NPCEntity attackerNPC = store.getComponent(attackerRef, NPCEntity.getComponentType());
            Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());

            // Case 2: NPC attacks Player
            if (attackerNPC != null && targetPlayer != null) {
                RecentOpponentCache.addOpponent(
                        targetPlayer.getUuid(),
                        attackerNPC.getRoleName(),
                        RecentOpponentCache.CombatState.WAS_ATTACKED);
                System.err.println("[BUD] Damage Event: " + attackerNPC.getRoleName() + " attacked player "
                        + targetPlayer.getUuid());
                return;
            }

        } catch (

        Exception e) {
            System.err.println("[BUD] Error in BudDamageFilterSystem: " + e.getMessage());
        }
    }
}
