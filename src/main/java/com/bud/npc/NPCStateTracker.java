package com.bud.npc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.HashSet;

import javax.annotation.Nonnull;

import com.bud.BudConfig;
import com.bud.interaction.BudChatInteraction;
import com.bud.interaction.BudSoundInteraction;
import com.bud.npcsound.IBudNPCSoundData;
import com.bud.llm.BudLLM;
import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.npcdata.IBudNPCData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

/**
 * Tracks Bud NPC interactions and sends LLM-generated chat messages when F-key is pressed.
 * Listens to PlayerInteractEvent with InteractionType.Use (F-key).
 * States: PetDefensive, PetPassive, PetSitting
 */
public class NPCStateTracker {
    
    private final BudConfig config;
    // Map of player UUID to their Bud NPC
    private final Map<UUID, Set<NPCEntity>> trackedBuds = new ConcurrentHashMap<>();
    // Map of player UUID to their PlayerRef for sending messages
    private final Map<UUID, PlayerRef> budOwners = new ConcurrentHashMap<>();
    // Map of player UUID to last known state (to detect changes)
    private final Map<UUID, String> lastKnownStates = new ConcurrentHashMap<>();
    // Map of NPC Ref to owner UUID (for reverse lookup on interaction)
    private final Map<Ref<EntityStore>, UUID> npcToOwner = new ConcurrentHashMap<>();
    // Map of NPC Ref to LLM Message
    private final Map<Ref<EntityStore>, IBudNPCData> npcToLLMMessage = new ConcurrentHashMap<>();
    private ScheduledFuture<?> pollingTask;
    
    private final BudLLM budLLM;

    private final BudChatInteraction chatInteraction = new BudChatInteraction();

    private final BudSoundInteraction soundInteraction = new BudSoundInteraction();
    
    public NPCStateTracker(BudConfig config) {
        this.config = config;
        this.budLLM = new BudLLM(config);
        System.out.println("[BUD] StateTracker initialized (polling based)");
    }

    /**
     * Start tracking a Bud for state changes.
     */
    public void trackBud(PlayerRef owner, NPCEntity bud, IBudNPCData budNPCData) {
        Ref<EntityStore> budRef = bud.getReference();
        if (budRef == null) {
            System.out.println("[BUD] Warning: Bud NPC has no valid reference!");
            return;
        }
        Role role = bud.getRole();
        if (role == null) {
            System.out.println("[BUD] Warning: Bud NPC has no valid Role!");
            return;
        }
        UUID ownerId = owner.getUuid();
        String stateName = getMainStateName(role.getStateSupport().getStateName());

        trackedBuds.computeIfAbsent(ownerId, k -> new HashSet<>()).add(bud);
        budOwners.put(ownerId, owner);
        npcToOwner.put(budRef, ownerId);
        npcToLLMMessage.put(budRef, budNPCData);
        lastKnownStates.put(ownerId, stateName);
        System.out.println("[BUD] Now tracking Bud for player " + ownerId + " - Initial state: " + stateName);

        // Start polling when at least one Bud is tracked
        startPolling();
    }
    
    /**
     * Stop tracking a Bud.
     */
    public void untrackBud(UUID ownerId) {
        Set<NPCEntity> buds = trackedBuds.remove(ownerId);
        budOwners.remove(ownerId);
        if (buds != null) {
            for (NPCEntity bud : buds) {
                Ref<EntityStore> budRef = bud.getReference();
                if (budRef != null) {
                    npcToOwner.remove(budRef);
                    npcToLLMMessage.remove(budRef);
                }
            }
        }
        lastKnownStates.remove(ownerId);
        System.out.println("[BUD] Stopped tracking Bud for player " + ownerId);

        if (trackedBuds.isEmpty()) {
            stopPolling();
        }
    }

    public synchronized void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::pollStates, 200L, 200L, TimeUnit.MILLISECONDS);
        System.out.println("[BUD] Started state polling task");
    }

    public synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
            System.out.println("[BUD] Stopped state polling task");
        }
    }
    
    private void pollStates() {
        if (trackedBuds.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Set<NPCEntity>> entry : trackedBuds.entrySet()) {
            UUID ownerId = entry.getKey();
            Set<NPCEntity> buds = entry.getValue();
            if (buds.isEmpty()) {
                untrackBud(ownerId);
                continue;
            }
            for (NPCEntity bud : buds) {
                Ref<EntityStore> ref = bud.getReference();
                if (ref == null) {
                    untrackBud(ownerId);
                    continue;
                }
    
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                world.execute(() -> checkStateChange(ownerId, bud));
            }

        }
    }

    private void checkStateChange(UUID ownerId, NPCEntity bud) {
        Role role = bud.getRole();
        if (role == null) {
            return;
        }

        String currentState = getMainStateName(role.getStateSupport().getStateName());
        String lastState = lastKnownStates.get(ownerId);

        if (lastState != null && lastState.equals(currentState)) {
            return;
        }

        lastKnownStates.put(ownerId, currentState);
        onStateChanged(ownerId, lastState, currentState);
    }
    
    /**
     * Extract main state name from full state string (e.g., "PetDefensive.Default" -> "PetDefensive")
     */
    private String getMainStateName(String fullStateName) {
        if (fullStateName == null) return "Unknown";
        int dotIndex = fullStateName.indexOf('.');
        return dotIndex > 0 ? fullStateName.substring(0, dotIndex) : fullStateName;
    }
    
    /**
     * Event handler for F-key interaction (InteractionType.Use).
     * Called when a player presses F on any entity.
     */
    @SuppressWarnings("deprecation")
    public void onPlayerInteraction(@Nonnull PlayerInteractEvent event) {
        // Debug: Log every interaction event
        System.out.println("[BUD] PlayerInteractEvent received! ActionType: " + event.getActionType());
        
        // Only handle F-key (Use) interactions
        if (event.getActionType() != InteractionType.Use) {
            System.out.println("[BUD] Ignoring non-Use interaction");
            return;
        }
        
        Entity targetEntity = event.getTargetEntity();
        System.out.println("[BUD] Target entity: " + (targetEntity != null ? targetEntity.getClass().getSimpleName() : "null"));
        
        if (targetEntity == null || !(targetEntity instanceof NPCEntity)) {
            System.out.println("[BUD] Target is not an NPC");
            return;
        }
        
        NPCEntity targetNPC = (NPCEntity) targetEntity;
        System.out.println("[BUD] Target NPC type: " + targetNPC.getNPCTypeId());
        
        Ref<EntityStore> targetRef = event.getTargetRef();
        if (targetRef == null && targetNPC.getReference() != null) {
            targetRef = targetNPC.getReference();
        }

        // Check if this NPC is a tracked Bud
        UUID ownerId = npcToOwner.get(targetRef);
        if (ownerId == null && targetNPC.getReference() != null) {
            ownerId = npcToOwner.get(targetNPC.getReference());
        }
        if (ownerId == null) {
            System.out.println("[BUD] F-key on NPC but not a tracked Bud (NPC: " + targetNPC.getNPCTypeId() + ")");
            return;
        }
        
        // Get the owner's player ref and check if the interacting player is the owner
        PlayerRef owner = budOwners.get(ownerId);
        if (owner == null) {
            return;
        }
        
        // Check if the interacting player is the owner
        Ref<EntityStore> playerEntityRef = event.getPlayerRef();
        Store<EntityStore> playerStore = playerEntityRef.getStore();
        PlayerRef interactingPlayerRef = playerStore.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (interactingPlayerRef == null) {
            System.out.println("[BUD] Could not resolve PlayerRef for interaction");
            return;
        }
        UUID interactingPlayerId = interactingPlayerRef.getUuid();
        if (!ownerId.equals(interactingPlayerId)) {
            System.out.println("[BUD] F-key on Bud but not by owner");
            return;
        }
        
        System.out.println("[BUD] ========================================");
        System.out.println("[BUD] F-KEY INTERACTION DETECTED!");
        System.out.println("[BUD] Player: " + interactingPlayerId);
        System.out.println("[BUD] ========================================");
        
        // Get current state and trigger message
        Role role = targetNPC.getRole();
        if (role != null) {
            String currentState = getMainStateName(role.getStateSupport().getStateName());
            String lastState = lastKnownStates.get(ownerId);
            
            System.out.println("[BUD] Current state: " + currentState + ", Last state: " + lastState);

            // Only react on actual state changes
            if (lastState != null && lastState.equals(currentState)) {
                System.out.println("[BUD] State unchanged; no message sent.");
                return;
            }

            // Update last known state
            lastKnownStates.put(ownerId, currentState);

            // Trigger LLM message
            onStateChanged(ownerId, lastState, currentState);
        }
    }

    /**
     * Interaction-system callback (Use NPC). This is the reliable path for F-key.
     */
    public void onNpcUsed(@Nonnull PlayerRef interactingPlayer, @Nonnull NPCEntity targetNPC) {
        Ref<EntityStore> targetRef = targetNPC.getReference();
        if (targetRef == null) {
            return;
        }

        UUID ownerId = npcToOwner.get(targetRef);
        if (ownerId == null) {
            System.out.println("[BUD] Use interaction on NPC but not a tracked Bud (NPC: " + targetNPC.getNPCTypeId() + ")");
            return;
        }

        if (!ownerId.equals(interactingPlayer.getUuid())) {
            System.out.println("[BUD] Use interaction on Bud but not by owner");
            return;
        }

        Role role = targetNPC.getRole();
        if (role == null) {
            return;
        }

        String currentState = getMainStateName(role.getStateSupport().getStateName());
        String lastState = lastKnownStates.get(ownerId);

        System.out.println("[BUD] (Interaction) Current state: " + currentState + ", Last state: " + lastState);

        if (lastState != null && lastState.equals(currentState)) {
            System.out.println("[BUD] (Interaction) State unchanged; no message sent.");
            return;
        }

        lastKnownStates.put(ownerId, currentState);
        onStateChanged(ownerId, lastState, currentState);
    }
    
    /**
     * Called when a Bud's state changes.
     */
    @SuppressWarnings("TooBroadCatch")
    private void onStateChanged(UUID ownerId, String fromState, String toState) {
        PlayerRef owner = budOwners.get(ownerId);
        if (owner == null) return;

        System.out.println("[BUD] State changed: " + fromState + " -> " + toState);
        Set<NPCEntity> buds = trackedBuds.get(ownerId);
        if (buds.isEmpty()) {
            System.out.println("[BUD] Warning: Bud NPC not found for owner " + ownerId);
            untrackBud(ownerId);
            return;
        }
        for (NPCEntity bud : buds) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null) {
                System.out.println("[BUD] Warning: Bud NPC has no valid reference!");
                untrackBud(ownerId);
                return;
            }
            IBudNPCData budNPCData = npcToLLMMessage.get(budRef);
            if (budNPCData == null) {
                System.out.println("[BUD] Warning: Bud NPC data not found!");
                return;
            }
            final Ref<EntityStore> ownerRef = owner.getReference();
            final World world = ownerRef != null ? ownerRef.getStore().getExternalData().getWorld() : null;
            if (world == null) {
                System.out.println("[BUD] Warning: Player world not available for sending messages!");
                return;
            }
            
            IBudNPCSoundData npcSoundData = budNPCData.getBudNPCSoundData();
            if (npcSoundData == null) {
                System.out.println("[BUD] Warning: Bud NPC sound data not found!");
                return;
            }
            String soundEventID = npcSoundData.getSoundForState(toState);
            this.soundInteraction.playSound(world, bud, soundEventID);
            
            // Get prompt for the new state
            ILLMBudNPCMessage npcMessage = budNPCData.getLLMBudNPCMessage();
            if (npcMessage == null) {
                System.out.println("[BUD] Warning: Bud NPC LLM message data not found!");
                return;
            }
            String prompt = npcMessage.getPromptForState(toState);
            
            if (budLLM != null && prompt != null && this.config.isEnableLLM()) {
                // Run LLM call async
                Thread.ofVirtual().start(() -> {
                    String message;
                    try {
                        String response = budLLM.callLLM(prompt);
                        message = npcMessage.getNPCName() + ": " + response;
                    } catch (java.io.IOException | InterruptedException e) {
                        System.out.println("[BUD] LLM error: " + e.getMessage());
                        message = npcMessage.getFallbackMessage(toState);
                    }
                    this.chatInteraction.sendChatMessage(world, owner, message);
                });
            } else {
                // No LLM configured or no prompt, send fallback
                String fallbackMessage = npcMessage.getFallbackMessage(toState);
                this.chatInteraction.sendChatMessage(world, owner, fallbackMessage);
            }
        }
    }

}
