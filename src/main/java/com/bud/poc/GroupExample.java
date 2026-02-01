package com.bud.poc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.group.EntityGroup;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/***
 * TODO: Remove this class in future
 */
@Deprecated
public class GroupExample {

    @Deprecated
    private static void addToEntityGroup(Store<EntityStore> store, PlayerRef playerRef, NPCEntity bud) {
        try {
            System.out.println("[BUD] Adding Bud and Player to EntityGroup...");

            Ref<EntityStore> playerStoreRef = playerRef.getReference();
            if (playerStoreRef == null || !playerStoreRef.isValid()) {
                System.err.println("[BUD] Cannot add to entity group: Player reference is invalid");
                return;
            }

            EntityGroup group = store.getComponent(playerStoreRef, EntityGroup.getComponentType());
            if (group == null) {
                System.out.println("[BUD] Creating new EntityGroup for player...");
                group = new EntityGroup();
                store.addComponent(playerStoreRef, EntityGroup.getComponentType(), group);
            }
            if (group != null) {
                System.out.println("[BUD] Player EntityGroup size: " + group.size());
            }

            Ref<EntityStore> budRef = bud.getReference();
            if (budRef != null && budRef.isValid()) {
                group.add(budRef);
                System.out.println("[BUD] Added Bud to Player's EntityGroup");

                EntityGroup group2 = store.getComponent(budRef, EntityGroup.getComponentType());
                if (group2 == null) {
                    group2 = new EntityGroup();
                    store.addComponent(budRef, EntityGroup.getComponentType(), group2);
                }
                group2.add(playerStoreRef);
                System.out.println("[BUD] Added Player to Bud's EntityGroup");
            } else {
                System.err.println("[BUD] Invalid Bud Reference!");
            }

        } catch (Exception e) {
            System.err.println("[BUD] Error adding to entity group: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
