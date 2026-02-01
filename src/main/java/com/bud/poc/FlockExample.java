package com.bud.poc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.flock.FlockMembership;
import com.hypixel.hytale.server.flock.FlockMembershipSystems;
import com.hypixel.hytale.server.flock.FlockPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/***
 * TODO: Remove this class in future
 */
@Deprecated
public class FlockExample {

    @Deprecated
    private static void addToFlock(Store<EntityStore> store, PlayerRef playerRef, NPCEntity bud) {
        try {
            System.out.println("[BUD] Adding Bud to Flock...");
            Ref<EntityStore> playerStoreRef = playerRef.getReference();
            if (playerStoreRef == null || !playerStoreRef.isValid()) {
                System.err.println("[BUD] Cannot add to flock: Player reference is invalid");
                return;
            }

            Ref<EntityStore> flockRef = null;
            FlockMembership playerMembership = store.getComponent(playerStoreRef, FlockMembership.getComponentType());

            System.out.println("[BUD] Checking if player is already in a flock...");
            if (playerMembership != null && playerMembership.getFlockRef() != null
                    && playerMembership.getFlockRef().isValid()) {
                flockRef = playerMembership.getFlockRef();
                System.out.println(
                        "[BUD] Player " + playerRef.getUsername() + " is already in flock: " + flockRef.toString());
            } else {
                System.out.println("[BUD] Creating new flock for player " + playerRef.getUsername());
                String[] allowedRoles = bud.getRole() != null ? bud.getRole().getFlockAllowedRoles() : new String[0];

                flockRef = FlockPlugin.createFlock(store, null, allowedRoles);
                System.out.println("[BUD] Created new flock: " + flockRef.toString());

                // Join Player to Flock
                FlockMembershipSystems.join(playerStoreRef, flockRef, store);

                // Upgrade Player to LEADER
                FlockMembership newMembership = store.getComponent(playerStoreRef, FlockMembership.getComponentType());
                if (newMembership != null) {
                    newMembership.setMembershipType(FlockMembership.Type.LEADER);
                    System.out.println("[BUD] Set Player " + playerRef.getUsername() + " as Flock LEADER");
                }
            }

            // 3. Add Bud to Flock
            FlockMembershipSystems.join(bud.getReference(), flockRef, store);
            System.out.println("[BUD] Added Bud " + bud.getNPCTypeId() + " to the flock.");

        } catch (Exception e) {
            System.err.println("[BUD] Error adding to flock: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
