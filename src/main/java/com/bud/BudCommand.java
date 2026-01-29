package com.bud;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.bud.npc.NPCStateTracker;
import com.bud.npc.NPCSpawner;
import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

import it.unimi.dsi.fastutil.Pair;


/**
 * This command spawns a Buddy that follows the player and can interact via LLM.
 * Uses a manual follow system that moves the NPC towards the player every tick.
 * Press F on the Bud to trigger LLM chat messages.
 */
public class BudCommand extends AbstractPlayerCommand {

    //private static final String FERAN_BUD = "Feran_Bud";

    private static final List<IBudNPCData> BUDS = List.of(
        new BudFeranData(),
        new BudTrorkData()
    );

    private static final ConcurrentHashMap<UUID, Set<NPCEntity>> spawnedBuds = new ConcurrentHashMap<>();

    private final NPCStateTracker stateTracker;
    
    public BudCommand(BudConfig config) {
        super("bud", "spawn bud.");
        this.stateTracker = new NPCStateTracker(config);
    }
    
    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        UUID id = playerRef.getUuid();

        if (playerHasValidBud(id, store)) {
            removeBudForOwner(id);
            return;
        }

        Vector3d position = getPlayerPosition(playerRef);
        Vector3f rotation = new Vector3f(0, 0, 0);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = null;

        for (IBudNPCData budNPCData : BUDS) {
            NPCEntity npc = null;
            try {
                result = NPCSpawner.create(store, budNPCData.getNPCTypeId(), position)
                    .withRotation(rotation)
                    .withInventory()
                    .addWeapon(budNPCData.getWeaponID(), 1, (short) 0)
                    .addArmor(budNPCData.getArmorID())
                    .spawn();
                
                if (result == null) {
                    System.out.println("[BUD] ✗ spawnNPC returned null!");
                    return;
                } else {
                    System.out.println("[BUD] ✓ Successfully spawned NPC!");
                }
                if (result.second() == null) {
                    System.out.println("[BUD] ✗ NPC instance is null, cannot print debug info.");
                    return;
                }
                npc = (NPCEntity) result.second();
            } catch (Exception e) {
                printError(e);
            }
    
            if (npc != null) {
                spawnedBuds.putIfAbsent(id, new HashSet<>());
                spawnedBuds.get(id).add(npc);
                stateTracker.trackBud(playerRef, npc, budNPCData);
                printNPCDebugInfo(npc);
            }
        }
    }

    public Set<String> getTrackedBudTypes() {
        Set<String> types = new HashSet<>();
        for (IBudNPCData budData : BUDS) {
            types.add(budData.getNPCTypeId());
        }
        return types;
    }
    
    public void removeBudForOwner(UUID ownerId) {
        Set<NPCEntity> buds = spawnedBuds.remove(ownerId);
        this.stateTracker.untrackBud(ownerId);

        for (NPCEntity bud : buds) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null) {
                continue;
            }
            Store<EntityStore> store = budRef.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> store.removeEntity(budRef, RemoveReason.REMOVE));
            System.out.println("[BUD] Removed Bud for player " + ownerId);
        }
    }
    
    public Set<Ref<EntityStore>> getTrackedBudRefs() {
        Set<Ref<EntityStore>> refs = new HashSet<>();
        for (Set<NPCEntity> npcs : spawnedBuds.values()) {
            for (NPCEntity npc : npcs) {
                if (npc == null) {
                    continue;
                }
                Ref<EntityStore> ref = npc.getReference();
                if (ref != null && ref.isValid()) {
                    refs.add(ref);
                }
            }
        }
        return refs;
    }

    private boolean playerHasValidBud(UUID playerId, Store<EntityStore> store) {
        if (!spawnedBuds.containsKey(playerId)) {
            return false;
        }
        Set<NPCEntity> buds = spawnedBuds.get(playerId);
        if (buds == null || buds.isEmpty()) {
            spawnedBuds.remove(playerId);
            return false;
        }
        boolean containsValid = false;
        for (NPCEntity bud : buds) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef != null && budRef.isValid()) {
                spawnedBuds.remove(playerId);
                containsValid = true;
                break;
            }
            boolean isDead = store.getArchetype(budRef).contains(DeathComponent.getComponentType());
            if (isDead) {
                spawnedBuds.remove(playerId);
            }
        }
        return containsValid;
    }

    private void printError(Exception e) {
        System.out.println("[BUD] ========================================");
        System.out.println("[BUD] SPAWN FAILED WITH EXCEPTION:");
        System.out.println("[BUD] Exception Type: " + e.getClass().getName());
        System.out.println("[BUD] Message: " + e.getMessage());
        System.out.println("[BUD] ========================================");
        
        // Try to extract more details from the cause chain
        Throwable cause = e.getCause();
        int depth = 1;
        while (cause != null && depth < 5) {
            System.out.println("[BUD] Cause " + depth + ": " + cause.getClass().getName());
            System.out.println("[BUD] Cause " + depth + " Message: " + cause.getMessage());
            cause = cause.getCause();
            depth++;
        }
    }

    private Vector3d getPlayerPosition(PlayerRef playerRef) {
        return playerRef.getTransform().getPosition();
    }

    /**
     * Prints detailed information about the NPC for debugging purposes.
     */
    private void printNPCDebugInfo(NPCEntity npc) {
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