package com.bud.npc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bud.npc.npcdata.IBudNPCData;
import com.bud.npc.npcdata.persistence.PersistenceManager;
import com.bud.result.DataListResult;
import com.bud.result.DataResult;
import com.bud.result.IDataListResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.bud.system.CleanUpHandler;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
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

    public static IDataListResult<NPCEntity> createBud(Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        Set<IBudNPCData> missingBuds = NPCManager.getInstance().getMissingBuds(playerRef.getUuid(), store);
        return createBud(store, playerRef, missingBuds);
    }

    public static IDataListResult<NPCEntity> createBud(Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            Set<IBudNPCData> missingBuds) {

        printPlayerDebugInfo(playerRef, store);

        List<NPCEntity> createdBuds = new ArrayList<>();
        for (IBudNPCData budNPCData : missingBuds) {
            try {
                DataResult<NPCEntity> spawnResult = spawnBud(store, playerRef, budNPCData);
                if (!spawnResult.isSuccess()) {
                    spawnResult.printResult();
                    continue;
                }
                NPCEntity npc = (NPCEntity) spawnResult.getData();
                IResult registerResult = NPCStateTracker.getInstance().registerBud(playerRef, npc, budNPCData);
                if (!registerResult.isSuccess()) {
                    CleanUpHandler.despawnBud(npc).printResult();
                    registerResult.printResult();
                    continue;
                }
                IResult persistResult = PersistenceManager.getInstance().persistBud(playerRef, npc);
                if (!persistResult.isSuccess()) {
                    CleanUpHandler.despawnBud(npc).printResult();
                    NPCStateTracker.getInstance().unregisterBud(npc).printResult();
                    persistResult.printResult();
                    continue;
                }
                createdBuds.add(npc);
                printNPCDebugInfo(npc);
            } catch (Exception e) {
                return new DataListResult<>(new HashSet<>(),
                        "Exception while spawning Bud " + budNPCData.getNPCTypeId() + ": " + e.getMessage());
            }

        }
        String joinedNames = createdBuds.stream()
                .map(npc -> npc.getNPCTypeId().split("_")[0])
                .collect(Collectors.joining(", "));
        return new DataListResult<>(createdBuds, "Created Buds: " + joinedNames);
    }

    private static DataResult<NPCEntity> spawnBud(Store<EntityStore> store, PlayerRef playerRef,
            IBudNPCData budNPCData) {
        try {
            Vector3d position = NPCManager.getInstance().getPlayerPositionWithOffset(playerRef);
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
                LoggerUtil.getLogger().fine(() -> "[BUD] Changed state to PetDefensive for NPC: " +
                        bud.getNPCTypeId());
            } else {
                LoggerUtil.getLogger().severe(() -> "[BUD] Could not find state 'PetDefensive' for NPC: " +
                        bud.getNPCTypeId());
            }

            // Set Player as LockedTarget (Standard slot for combat targets)
            MarkedEntitySupport markedSupport = role.getMarkedEntitySupport();
            if (markedSupport != null) {
                markedSupport.setMarkedEntity("LockedTarget", owner.getReference());
                LoggerUtil.getLogger().fine(() -> "[BUD] Set player as LockedTarget");
            }

        });
        return new SuccessResult("Changed state to " + bud.getRole().getStateSupport().getStateName());
    }

    private static void printPlayerDebugInfo(PlayerRef playerRef, Store<EntityStore> store) {
        LoggerUtil.getLogger().fine(() -> "======= BUD PLAYER DEBUG INFO =======");
        LoggerUtil.getLogger().fine(() -> "Player Name: " + playerRef.getUsername());
        LoggerUtil.getLogger().fine(() -> "Player UUID: " + playerRef.getUuid());

        if (store != null) {
            LoggerUtil.getLogger().fine(() -> "Store Class: " + store.getClass().getName());
            try {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null) {
                    LoggerUtil.getLogger().fine(() -> "Player Store Ref: " + ref.toString() + " (Valid: " + ref.isValid() + ")");

                    // Try to get EntityGroup
                    EntityGroup group = store.getComponent(ref, EntityGroup.getComponentType());
                    if (group != null) {
                        LoggerUtil.getLogger().fine(() -> "Player EntityGroup: Present (Size: " + group.size() + ")");
                    } else {
                        LoggerUtil.getLogger().warning(() -> "Player EntityGroup: NULL");
                    }
                } else {
                    LoggerUtil.getLogger().warning(() -> "Player Store Ref is NULL");
                }
            } catch (Exception e) {
                LoggerUtil.getLogger().warning(() -> "[BudPlugin] Error identifying player components: " + e.getMessage());
            }
        } else {
            LoggerUtil.getLogger().warning(() -> "[BudPlugin] Store is null.");
        }
        LoggerUtil.getLogger().fine(() -> "=====================================");
    }

    private static void printNPCDebugInfo(NPCEntity npc) {
        LoggerUtil.getLogger().fine(() -> "======= BUD NPC DEBUG INFO =======");
        LoggerUtil.getLogger().fine(() -> "NPC Name: " + npc.getNPCTypeId());
        LoggerUtil.getLogger().fine(() -> "Role Name: " + npc.getRoleName());
        Role role = npc.getRole();
        if (role != null) {
            LoggerUtil.getLogger().fine(() -> "--- AI & Behavior ---");
            LoggerUtil.getLogger().fine(() -> "Can Lead Flock: " + role.isCanLeadFlock());
            LoggerUtil.getLogger().fine(() -> "Is Avoiding Entities: " + role.isAvoidingEntities());

            // Attitude Info
            LoggerUtil.getLogger().fine(() -> "Default Player Attitude: " + role.getWorldSupport().getDefaultPlayerAttitude());
            LoggerUtil.getLogger().fine(() -> "Default NPC Attitude: " + role.getWorldSupport().getDefaultNPCAttitude());

            // Damage Groups Info
            LoggerUtil.getLogger().fine(() -> "--- Damage Settings ---");
            var combatSupport = role.getCombatSupport();
            if (combatSupport != null) {
                int[] disableGroups = combatSupport.getDisableDamageGroups();
                if (disableGroups != null) {
                    LoggerUtil.getLogger().fine(() -> "DisableDamageGroups (Count): " + disableGroups.length);
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
                        LoggerUtil.getLogger().severe(() -> "Reverse lookup debug error: " + e);
                    }
                    // ----------------------------

                    for (int g : disableGroups) {
                        String groupName = "Unknown";
                        var groupAsset = assetMap.getAsset(g);
                        if (groupAsset != null) {
                            groupName = groupAsset.getId();
                            final String logGroupName = groupName;
                            LoggerUtil.getLogger().fine(() -> " - Group ID: " + g + " (" + logGroupName + ")");
                        } else if (reverseMap.containsKey(g)) {
                            groupName = reverseMap.get(g) + " (Mapped)";
                            final String logGroupName = groupName;
                            LoggerUtil.getLogger().fine(() -> " - Group ID: " + g + " (" + logGroupName + ")");
                        }
                    }
                } else {
                    LoggerUtil.getLogger().warning(() -> "DisableDamageGroups is NULL");
                }

                LoggerUtil.getLogger().fine(() -> "Is Dealing Friendly Damage: " + combatSupport.isDealingFriendlyDamage());
            }

            // Check if it's friendly now
            LoggerUtil.getLogger().fine(() -> "--- Current Status ---");
            LoggerUtil.getLogger().fine(() -> "Is Backing Away: " + role.isBackingAway());

            // NEW: Print available states
            LoggerUtil.getLogger().fine(() -> "--- Available States ---");
            try {
                var stateHelper = role.getStateSupport().getStateHelper();
                LoggerUtil.getLogger().fine(() -> "Current State: " + role.getStateSupport().getStateName());
                // Try to get state indices for common states
                int idleIdx = stateHelper.getStateIndex("Idle");
                int petPassiveIdx = stateHelper.getStateIndex("PetPassive");
                int petDefensiveIdx = stateHelper.getStateIndex("PetDefensive");
                int petSittingIdx = stateHelper.getStateIndex("PetSitting");
                LoggerUtil.getLogger().fine(() -> "State 'Idle' index: " + (idleIdx != Integer.MIN_VALUE ? idleIdx : "NOT FOUND"));
                LoggerUtil.getLogger().fine(() -> "State 'PetPassive' index: "
                        + (petPassiveIdx != Integer.MIN_VALUE ? petPassiveIdx : "NOT FOUND"));
                LoggerUtil.getLogger().fine(() -> "State 'PetDefensive' index: "
                        + (petDefensiveIdx != Integer.MIN_VALUE ? petDefensiveIdx : "NOT FOUND"));
                LoggerUtil.getLogger().fine(() -> "State 'PetSitting' index: "
                        + (petSittingIdx != Integer.MIN_VALUE ? petSittingIdx : "NOT FOUND"));
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "Error reading states: " + e.getMessage());
            }
        }

        LoggerUtil.getLogger().fine(() -> "==================================");
    }

}