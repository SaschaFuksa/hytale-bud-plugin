package com.bud;

import java.util.Set;
import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.bud.npc.NPCManager;
import com.bud.npc.NPCSpawner;
import com.bud.npcdata.IBudNPCData;
import com.bud.result.IResult;
import com.bud.system.CleanUpHandler;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
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

    public BudCommand(BudPlugin budPlugin) {
        super("bud", "spawn bud.");
        this.addUsageVariant(new BudSetVariant());
    }
    
    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        UUID id = playerRef.getUuid();

        Set<IBudNPCData> missingBuds = NPCManager.getMissingBuds(id, store);

        Vector3d position = getPlayerPosition(playerRef);
        Vector3f rotation = new Vector3f(0, 0, 0);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = null;

        for (IBudNPCData budNPCData : missingBuds) {
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
                IResult spawnResult = NPCManager.addSpawnedBud(playerRef, budNPCData, npc);
                spawnResult.printResult();
                printNPCDebugInfo(npc);
            }
        }
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

    private static class BudSetVariant extends AbstractPlayerCommand {

        private final RequiredArg<String> modeArg;

        public BudSetVariant() {
            super("Manage Bud NPCs");
            this.modeArg = this.withRequiredArg("mode", "clean or clean-all", ArgTypes.STRING);
        }

        @Override
        protected void execute(@NonNullDecl CommandContext commandContext,
                @NonNullDecl Store<EntityStore> store,
                @NonNullDecl Ref<EntityStore> ref,
                @NonNullDecl PlayerRef playerRef,
                @NonNullDecl World world) {
            
            String inputMode = this.modeArg.get(commandContext);

            if (inputMode.equalsIgnoreCase("clean")) {
                IResult result = CleanUpHandler.removeOwnerBuds(playerRef);
                result.printResult();
            } else if (inputMode.equalsIgnoreCase("clean-all")) {
                IResult result = CleanUpHandler.removeAllBuds(world);
                result.printResult();
            } else {
                System.out.println("Unknown mode: " + inputMode);
            }
        }

    }

}