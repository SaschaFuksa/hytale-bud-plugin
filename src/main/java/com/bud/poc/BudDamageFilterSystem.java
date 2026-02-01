package com.bud.poc;

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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/***
 * TODO: Remove this class in future
 */
@Deprecated
public class BudDamageFilterSystem extends DamageEventSystem {

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Optimized: Only run this system for entities that are Players (have PlayerRef
        // component)
        return PlayerRef.getComponentType();
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

            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            Player targetPlayer = store.getComponent(targetRef, Player.getComponentType());
            System.err.println("[BUD] Damage Event: Attacker="
                    + (attackerPlayer != null ? attackerPlayer.getDisplayName() : "Non-Player")
                    + ", Target=" + (targetPlayer != null ? targetPlayer.getDisplayName() : "Non-Player")
                    + ", Amount=" + damage.getAmount());

            PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) {
                return;
            }

            if (attackerPlayer == null && targetPlayer != null) {
                System.out.println(
                        "[BUD] Filter: Blocked entity attack on Player " + playerRef.getUuid());
                damage.setAmount(damage.getAmount() * 0.1f);
                // damage.setSource(null);
                // damage.setCancelled(true);
            }
            // TODO
            // Implement short memory of targets and after some time allow bud chat to talk
            // about
            // Damage.Source source = damage.getSource();
            // if (source instanceof Damage.EntitySource entitySource) {
            // Ref<?> sourceRefRaw = entitySource.getRef();

            // if (sourceRefRaw != null && sourceRefRaw.isValid()) {
            // try {
            // @SuppressWarnings("unchecked")
            // Ref<EntityStore> attackerRef = (Ref<EntityStore>) sourceRefRaw;

            // // If the attacker is a Bud owned by the victim (player), cancel the damage
            // if (NPCManager.getInstance().isBudOwnedBy(playerRef.getUuid(), attackerRef))
            // {
            // System.out.println(
            // "[BUD] Filter: Blocked Friendly Fire from Bud to Player " +
            // playerRef.getUuid());
            // // damage.setCancelled(true);
            // }
            // } catch (ClassCastException ignored) {
            // // Not an EntityStore ref
            // }
            // }
        } catch (

        Exception e) {
            System.err.println("[BUD] Error in BudDamageFilterSystem: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
