package com.bud.system;

import com.bud.npc.NPCManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

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
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        if (damage.isCancelled()) {
            return;
        }

        Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
        if (victimRef == null || !victimRef.isValid()) {
            return;
        }

        PlayerRef playerRef = null;
        try {
            if (archetypeChunk.getArchetype().contains(PlayerRef.getComponentType())) {
                playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
            }
        } catch (Exception e) {
            // Ignore
        }

        if (playerRef == null) {
            // Victim is not a player
            return;
        }

        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<?> sourceRefRaw = entitySource.getRef();
            if (sourceRefRaw == null || !sourceRefRaw.isValid()) {
                return;
            }

            try {
                @SuppressWarnings("unchecked")
                Ref<EntityStore> attackerRef = (Ref<EntityStore>) sourceRefRaw;
                
                boolean isOwnBud = NPCManager.getInstance().isBudOwnedBy(playerRef.getUuid(), attackerRef);
                
                if (isOwnBud) {
                    System.out.println("[BUD] Detailed Damage Filter: Blocked Friendly Fire from Bud to Player " + playerRef.getUuid() + " | DmgObj=" + System.identityHashCode(damage));
                    damage.setCancelled(true);
                }
            } catch (ClassCastException e) {
                // Source wasn't an entity store ref
            }
        }
    }
}
