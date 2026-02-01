package com.bud.npc;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.npcdata.IBudNPCData;
import com.bud.npcdata.persistence.PersistenceManager;
import com.bud.result.DataResult;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.bud.system.CleanUpHandler;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import com.hypixel.hytale.server.core.entity.group.EntityGroup;

import it.unimi.dsi.fastutil.Pair;

public class BudCreation {

    public static IResult createBud(Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        Set<IBudNPCData> missingBuds = NPCManager.getInstance().getMissingBuds(playerRef.getUuid(), store);
        return createBud(store, playerRef, missingBuds);
    }

    public static IResult createBud(Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            Set<IBudNPCData> missingBuds) {

        printPlayerDebugInfo(playerRef, store);

        List<String> createdBuds = new ArrayList<>();
        for (IBudNPCData budNPCData : missingBuds) {
            try {
                DataResult<NPCEntity> spawnResult = spawnBud(store, playerRef, budNPCData);
                if (!spawnResult.isSuccess()) {
                    return spawnResult;
                }
                NPCEntity npc = (NPCEntity) spawnResult.getData();
                IResult registerResult = NPCStateTracker.getInstance().registerBud(playerRef, npc, budNPCData);
                if (!registerResult.isSuccess()) {
                    CleanUpHandler.despawnBud(npc).printResult();
                    return registerResult;
                }
                IResult persistResult = PersistenceManager.getInstance().persistBud(playerRef, npc);
                if (!persistResult.isSuccess()) {
                    CleanUpHandler.despawnBud(npc).printResult();
                    NPCStateTracker.getInstance().unregisterBud(npc).printResult();
                    return persistResult;
                }
                createdBuds.add(budNPCData.getNPCDisplayName());
                printNPCDebugInfo(npc);
            } catch (Exception e) {
                return new ErrorResult(
                        "Exception while spawning Bud " + budNPCData.getNPCTypeId() + ": " + e.getMessage());
            }

        }
        return new SuccessResult("Created Buds: " + String.join(", ", createdBuds));
    }

    private static DataResult<NPCEntity> spawnBud(Store<EntityStore> store, PlayerRef playerRef,
            IBudNPCData budNPCData) {
        try {
            Vector3d position = NPCManager.getInstance().getPlayerPosition(playerRef);
            Vector3f rotation = new Vector3f(0, 0, 0);
            Pair<Ref<EntityStore>, INonPlayerCharacter> result = NPCSpawner
                    .create(store, budNPCData.getNPCTypeId(), position)
                    .withRotation(rotation)
                    .withInventory()
                    .addWeapon(budNPCData.getWeaponID(), 1, (short) 0)
                    .addArmor(budNPCData.getArmorID())
                    .spawn();
            NPCEntity npc = (NPCEntity) result.second();
            changeRoleState(npc, playerRef).printResult();
            return new DataResult<>(npc,
                    "Spawned Bud " + budNPCData.getNPCTypeId() + " for player " + playerRef.getUuid());
        } catch (Exception e) {
            return new DataResult<>(null,
                    "Exception while spawning Bud " + budNPCData.getNPCTypeId() + ": " + e.getMessage());
        }
    }

    private static IResult changeRoleState(NPCEntity bud, PlayerRef owner) {
        Role role = bud.getRole();
        bud.getWorld().execute(() -> {
            StateSupport stateSupport = role.getStateSupport();
            int attackStateIndex = stateSupport.getStateHelper().getStateIndex("PetDefensive");

            if (attackStateIndex >= 0) {
                // Use default sub-state for Attack
                String defaultSubStateName = stateSupport.getStateHelper().getDefaultSubState();
                int subStateIndex = stateSupport.getStateHelper().getSubStateIndex(attackStateIndex,
                        defaultSubStateName);

                // Force the state change
                stateSupport.setState(attackStateIndex, subStateIndex, true, false);
                System.out.println("[BUD] Force-set state to PetDefensive");
            } else {
                System.err.println("[BUD] Could not find state 'PetDefensive' for NPC: " +
                        bud.getNPCTypeId());
            }

            // Set Player as LockedTarget (Standard slot for combat targets)
            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
            if (markedSupport != null) {
                markedSupport.setMarkedEntity("LockedTarget", owner.getReference());
                System.out.println("[BUD] Set player as LockedTarget");
            }

        });
        return new SuccessResult("Changed state to " + bud.getRole().getStateSupport().getStateName());
    }

    private static void printPlayerDebugInfo(PlayerRef playerRef, Store<EntityStore> store) {
        System.out.println("======= BUD PLAYER DEBUG INFO =======");
        System.out.println("Player Name: " + playerRef.getUsername());
        System.out.println("Player UUID: " + playerRef.getUuid());

        if (store != null) {
            System.out.println("Store Class: " + store.getClass().getName());
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null) {
                    System.out.println("Player Store Ref: " + ref.toString() + " (Valid: " + ref.isValid() + ")");

                    // Try to get EntityGroup
                    EntityGroup group = store.getComponent(ref, EntityGroup.getComponentType());
                    if (group != null) {
                        System.out.println("Player EntityGroup: Present (Size: " + group.size() + ")");
                    } else {
                        System.out.println("Player EntityGroup: NULL");
                    }
                } else {
                    System.out.println("Player Store Ref is NULL");
                }
            } catch (Exception e) {
                System.out.println("[BudPlugin] Error identifying player components: " + e.getMessage());
            }
        } else {
            System.out.println("[BudPlugin] Store is null.");
        }
        System.out.println("=====================================");
    }

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

            // Damage Groups Info
            System.out.println("--- Damage Settings ---");
            var combatSupport = role.getCombatSupport();
            if (combatSupport != null) {
                int[] disableGroups = combatSupport.getDisableDamageGroups();
                if (disableGroups != null) {
                    System.out.println("DisableDamageGroups (Count): " + disableGroups.length);
                    var assetMap = com.hypixel.hytale.server.npc.config.AttitudeGroup.getAssetMap();

                    // --- REVERSE LOOKUP DEBUG ---
                    java.util.Map<Integer, String> reverseMap = new java.util.HashMap<>();
                    try {
                        java.util.Map<?, ?> rawMap = assetMap.getAssetMap();
                        if (rawMap != null) {
                            for (java.util.Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                Object key = entry.getKey();
                                Object val = entry.getValue();
                                if (val instanceof Integer) {
                                    reverseMap.put((Integer) val, String.valueOf(key));
                                } else {
                                    // Try to get index from key
                                    try {
                                        int id = assetMap.getIndex((String) key);
                                        reverseMap.put(id, String.valueOf(key));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Reverse lookup debug error: " + e);
                    }
                    // ----------------------------

                    for (int g : disableGroups) {
                        String groupName = "Unknown";
                        var groupAsset = assetMap.getAsset(g);
                        if (groupAsset != null) {
                            groupName = groupAsset.getId();
                        } else if (reverseMap.containsKey(g)) {
                            groupName = reverseMap.get(g) + " (Mapped)";
                        }
                        System.out.println(" - Group ID: " + g + " (" + groupName + ")");
                    }
                } else {
                    System.out.println("DisableDamageGroups is NULL");
                }

                System.out.println("Is Dealing Friendly Damage: " + combatSupport.isDealingFriendlyDamage());
            }

            // Check if it's friendly now
            System.out.println("--- Current Status ---");
            System.out.println("Is Backing Away: " + role.isBackingAway());

            // NEW: Print available states
            System.out.println("--- Available States ---");
            try {
                var stateHelper = role.getStateSupport().getStateHelper();
                System.out.println("Current State: " + role.getStateSupport().getStateName());
                // Try to get state indices for common states
                int idleIdx = stateHelper.getStateIndex("Idle");
                int petPassiveIdx = stateHelper.getStateIndex("PetPassive");
                int petDefensiveIdx = stateHelper.getStateIndex("PetDefensive");
                int petSittingIdx = stateHelper.getStateIndex("PetSitting");
                System.out.println("State 'Idle' index: " + (idleIdx != Integer.MIN_VALUE ? idleIdx : "NOT FOUND"));
                System.out.println("State 'PetPassive' index: "
                        + (petPassiveIdx != Integer.MIN_VALUE ? petPassiveIdx : "NOT FOUND"));
                System.out.println("State 'PetDefensive' index: "
                        + (petDefensiveIdx != Integer.MIN_VALUE ? petDefensiveIdx : "NOT FOUND"));
                System.out.println("State 'PetSitting' index: "
                        + (petSittingIdx != Integer.MIN_VALUE ? petSittingIdx : "NOT FOUND"));
            } catch (Exception e) {
                System.out.println("Error reading states: " + e.getMessage());
            }
        }

        System.out.println("==================================");
    }

}