package com.bud.core.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jspecify.annotations.NonNull;

import com.bud.core.components.PlayerBudComponent;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.group.EntityGroup;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.StateMappingHelper;
import com.hypixel.hytale.server.npc.config.AttitudeGroup;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.CombatSupport;

public class BudDebugInfo {

    private static final BudDebugInfo INSTANCE = new BudDebugInfo();

    private BudDebugInfo() {
    }

    public static BudDebugInfo getInstance() {
        return INSTANCE;
    }

    public void logPlayerInfo(@NonNull PlayerRef playerRef, @NonNull Store<EntityStore> store) {
        LoggerUtil.getLogger().fine(() -> "======= BUD PLAYER DEBUG INFO =======");
        LoggerUtil.getLogger().fine(() -> "Player Name: " + playerRef.getUsername());
        LoggerUtil.getLogger().fine(() -> "Player UUID: " + playerRef.getUuid());
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                LoggerUtil.getLogger()
                        .fine(() -> "Player Store Ref: " + ref.toString() + " (Valid: " + ref.isValid() + ")");

                ComponentType<EntityStore, EntityGroup> entityGroupType = EntityGroup.getComponentType();
                if (entityGroupType != null) {
                    EntityGroup group = store.getComponent(ref, entityGroupType);
                    if (group != null) {
                        LoggerUtil.getLogger().fine(() -> "Player EntityGroup: Present (Size: " + group.size() + ")");
                    } else {
                        LoggerUtil.getLogger().warning(() -> "Player EntityGroup: NULL");
                    }
                } else {
                    LoggerUtil.getLogger().warning(() -> "Could not retrieve EntityGroup component type.");
                }
                PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent.getComponentType());
                if (playerBudComponent != null) {
                    playerBudComponent.getBudTypes().forEach(budType -> LoggerUtil.getLogger()
                            .fine(() -> "Player has Bud of type: " + budType));
                } else {
                    LoggerUtil.getLogger().warning(() -> "PlayerBudComponent: NULL");
                }
            } else {
                LoggerUtil.getLogger().warning(() -> "Player Store Ref is NULL or Invalid.");
            }
        } catch (Exception e) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BudPlugin] Error identifying player components: " + e.getMessage());
        }

        LoggerUtil.getLogger().fine(() -> "=====================================");
    }

    public void logBudInfo(@NonNull NPCEntity npc) {
        LoggerUtil.getLogger().fine(() -> "======= BUD NPC DEBUG INFO =======");
        LoggerUtil.getLogger().fine(() -> "NPC Name: " + npc.getNPCTypeId());
        LoggerUtil.getLogger().fine(() -> "Role Name: " + npc.getRoleName());
        Role role = npc.getRole();
        if (role != null) {
            LoggerUtil.getLogger().fine(() -> "--- Bud Infos ---");
            LoggerUtil.getLogger().fine(() -> "Can Lead Flock: " + role.isCanLeadFlock());
            LoggerUtil.getLogger().fine(() -> "Is Avoiding Entities: " + role.isAvoidingEntities());

            // Attitude Info
            LoggerUtil.getLogger()
                    .fine(() -> "Default Player Attitude: " + role.getWorldSupport().getDefaultPlayerAttitude());
            LoggerUtil.getLogger()
                    .fine(() -> "Default NPC Attitude: " + role.getWorldSupport().getDefaultNPCAttitude());

            // Damage Groups Info
            LoggerUtil.getLogger().fine(() -> "--- Damage Settings ---");
            CombatSupport combatSupport = role.getCombatSupport();
            int[] disableGroups = combatSupport.getDisableDamageGroups();
            if (disableGroups != null) {
                LoggerUtil.getLogger().fine(() -> "DisableDamageGroups (Count): " + disableGroups.length);
                IndexedLookupTableAssetMap<String, AttitudeGroup> assetMap = AttitudeGroup.getAssetMap();

                // --- REVERSE LOOKUP DEBUG ---
                Map<Integer, String> reverseMap = new HashMap<>();
                try {
                    Map<?, ?> rawMap = assetMap.getAssetMap();
                    for (Entry<?, ?> entry : rawMap.entrySet()) {
                        Object key = entry.getKey();
                        Object val = entry.getValue();
                        if (val instanceof Integer intVal) {
                            reverseMap.put(intVal, String.valueOf(key));
                        } else {
                            // Try to get index from key
                            try {
                                int id = assetMap.getIndex((String) key);
                                reverseMap.put(id, String.valueOf(key));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "Reverse lookup debug error: " + e);
                }
                // ----------------------------

                for (int g : disableGroups) {
                    String logGroupName;
                    AttitudeGroup groupAsset = assetMap.getAsset(g);
                    if (groupAsset != null) {
                        logGroupName = groupAsset.getId();
                        final String finalLogGroupName = logGroupName;
                        LoggerUtil.getLogger().fine(() -> " - Group ID: " + g + " (" + finalLogGroupName + ")");
                    } else if (reverseMap.containsKey(g)) {
                        logGroupName = reverseMap.get(g) + " (Mapped)";
                        final String finalLogGroupName = logGroupName;
                        LoggerUtil.getLogger().fine(() -> " - Group ID: " + g + " (" + finalLogGroupName + ")");
                    }
                }
            } else {
                LoggerUtil.getLogger().warning(() -> "DisableDamageGroups is NULL");
            }

            LoggerUtil.getLogger()
                    .fine(() -> "Is Dealing Friendly Damage: " + combatSupport.isDealingFriendlyDamage());

            // Check if it's friendly now
            LoggerUtil.getLogger().fine(() -> "--- Current Status ---");
            LoggerUtil.getLogger().fine(() -> "Is Backing Away: " + role.isBackingAway());

            LoggerUtil.getLogger().fine(() -> "--- Available States ---");
            try {
                StateMappingHelper stateHelper = role.getStateSupport().getStateHelper();
                LoggerUtil.getLogger().fine(() -> "Current State: " + role.getStateSupport().getStateName());
                // Try to get state indices for common states
                int idleIdx = stateHelper.getStateIndex("Idle");
                int petPassiveIdx = stateHelper.getStateIndex("PetPassive");
                int petDefensiveIdx = stateHelper.getStateIndex("PetDefensive");
                int petSittingIdx = stateHelper.getStateIndex("PetSitting");
                LoggerUtil.getLogger()
                        .fine(() -> "State 'Idle' index: " + (idleIdx != Integer.MIN_VALUE ? idleIdx : "NOT FOUND"));
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
