package com.bud.system;

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
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.core.entity.group.EntityGroup;

import javax.annotation.Nonnull;

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
            Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) {
                return;
            }

            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef == null || !attackerRef.isValid()) {
                return;
            }

            // 1. Check Flock Membership (Bidirectional)
            FlockMembership victimFlock = store.getComponent(victimRef, FlockMembership.getComponentType());
            FlockMembership attackerFlock = store.getComponent(attackerRef, FlockMembership.getComponentType());

            if (victimFlock != null && attackerFlock != null) {
                Ref<EntityStore> vFlockRef = victimFlock.getFlockRef();
                Ref<EntityStore> aFlockRef = attackerFlock.getFlockRef();

                if (vFlockRef != null && vFlockRef.isValid() && vFlockRef.equals(aFlockRef)) {
                    // Prevent friendly fire in same flock
                    damage.setAmount(0);
                    return;
                }
            }

            // 2. Check EntityGroup (Unidirectional Check)
            // Note: block commented out until EntityGroup API for containment is confirmed.
            /*
             * if (isMemberOfGroup(store, attackerRef, victimRef) || isMemberOfGroup(store,
             * victimRef, attackerRef)) {
             * damage.setAmount(0);
             * return;
             * }
             */

        } catch (Exception e) {
            System.err.println("[BUD] Error in BudDamageFilterSystem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Placeholder until EntityGroup API is known
    // private boolean isMemberOfGroup(Store<EntityStore> store, Ref<EntityStore>
    // owner, Ref<EntityStore> member) {
    // EntityGroup group = store.getComponent(owner,
    // EntityGroup.getComponentType());
    // if (group != null) {
    // return group.contains(member);
    // }
    // return false;
    // }
}
