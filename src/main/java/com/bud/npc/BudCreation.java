package com.bud.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bud.npcdata.IBudNPCData;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

import it.unimi.dsi.fastutil.Pair;

public class BudCreation {

    public static IResult createBud(Store<EntityStore> store, PlayerRef playerRef) {
        Set<IBudNPCData> missingBuds = NPCManager.getMissingBuds(playerRef.getUuid(), store);
        return createBud(store, playerRef, missingBuds);
    }

    public static IResult createBud(Store<EntityStore> store, PlayerRef playerRef, Set<IBudNPCData> missingBuds) {
        Vector3d position = NPCManager.getPlayerPosition(playerRef);
        Vector3f rotation = new Vector3f(0, 0, 0);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = null;
        List<String> createdBuds = new ArrayList<>();
        for (IBudNPCData budNPCData : missingBuds) {
            try {
                result = NPCSpawner.create(store, budNPCData.getNPCTypeId(), position)
                    .withRotation(rotation)
                    .withInventory()
                    .addWeapon(budNPCData.getWeaponID(), 1, (short) 0)
                    .addArmor(budNPCData.getArmorID())
                    .spawn();
                
                if (result == null) {
                    return new ErrorResult("Failed to spawn NPC: result is null.");
                }
                if (result.second() == null) {
                    return new ErrorResult("Failed to spawn NPC: NPC instance is null.");
                }
                NPCEntity npc = (NPCEntity) result.second();
                IResult spawnResult = NPCManager.addSpawnedBud(playerRef, budNPCData, npc);
                if(!spawnResult.isSuccess()) {
                    return spawnResult;
                }
                createdBuds.add(budNPCData.getNPCTypeId());
                printNPCDebugInfo(npc);
            } catch (Exception e) {
                return new ErrorResult("Exception while spawning Bud " + budNPCData.getNPCTypeId() + ": " + e.getMessage());
            }
    
        }
        return new SuccessResult("Created Buds: " + String.join(", ", createdBuds));
    }

    /**
     * Prints detailed information about the NPC for debugging purposes.
     */
    private static void printNPCDebugInfo(NPCEntity npc) {
        System.out.println("======= BUD NPC DEBUG INFO =======");
        System.out.println("NPC Name: " + npc.getNPCTypeId());
        System.out.println("Role Name: " + npc.getRoleName());
        Role role = npc.getRole();
        if (role != null) {
            System.out.println("--- AI & Behavior ---");
            System.out.println("Can Lead Flock: " + role.isCanLeadFlock());
            System.out.println("Is Avoiding Entities: " + role.isAvoidingEntities());
            
            // Attitude Info
            System.out.println("Default Player Attitude: " + role.getWorldSupport().getDefaultPlayerAttitude());
            System.out.println("Default NPC Attitude: " + role.getWorldSupport().getDefaultNPCAttitude());
            
            // Check if it's friendly now
            System.out.println("--- Current Status ---");
            System.out.println("Is Backing Away: " + role.isBackingAway());
            
            // NEW: Print available states
            System.out.println("--- Available States ---");
            try {
                var stateHelper = role.getStateSupport().getStateHelper();
                System.out.println("Current State: " + role.getStateSupport().getStateName()
            );
                // Try to get state indices for common states
                int idleIdx = stateHelper.getStateIndex("Idle");
                int petPassiveIdx = stateHelper.getStateIndex("PetPassive");
                int petDefensiveIdx = stateHelper.getStateIndex("PetDefensive");
                int petSittingIdx = stateHelper.getStateIndex("PetSitting");
                System.out.println("State 'Idle' index: " + (idleIdx != Integer.MIN_VALUE ? idleIdx : "NOT FOUND"));
                System.out.println("State 'PetPassive' index: " + (petPassiveIdx != Integer.MIN_VALUE ? petPassiveIdx : "NOT FOUND"));
                System.out.println("State 'PetDefensive' index: " + (petDefensiveIdx != Integer.MIN_VALUE ? petDefensiveIdx : "NOT FOUND"));
                System.out.println("State 'PetSitting' index: " + (petSittingIdx != Integer.MIN_VALUE ? petSittingIdx : "NOT FOUND"));
            } catch (Exception e) {
                System.out.println("Error reading states: " + e.getMessage());
            }
        }
        
        System.out.println("==================================");
    }
    
}