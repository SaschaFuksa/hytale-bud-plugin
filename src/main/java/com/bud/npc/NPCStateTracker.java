package com.bud.npc;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.systems.BudTimeInformation;
import com.bud.systems.BudWorldContext;
import com.bud.systems.BudWorldInformation;
import com.bud.systems.TimeOfDay;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

import com.bud.interaction.BudChatInteraction;
import com.bud.interaction.BudSoundInteraction;
import com.bud.npcsound.IBudNPCSoundData;
import com.bud.llm.BudLLM;
import com.bud.llmmessages.ILLMBudNPCMessage;
import com.bud.llmworldmessages.LLMWorldInfoMessageManager;
import com.bud.npcdata.IBudNPCData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
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
    
    private ScheduledFuture<?> pollingTask;
    
    private final BudLLM budLLM;

    private final BudChatInteraction chatInteraction = new BudChatInteraction();

    private final BudSoundInteraction soundInteraction = new BudSoundInteraction();
    
    public NPCStateTracker() {
        this.budLLM = new BudLLM();
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

        // Auto-Follow: Set state to PetDefensive immediately
        try {
            // role.getStateSupport().setState("PetDefensive");
            System.out.println("[BUD] Auto-assigned PetDefensive (Follow) state to new Bud.");
        } catch (Exception e) {
            System.err.println("[BUD] Failed to set auto-follow state: " + e.getMessage());
        }

        UUID ownerId = owner.getUuid();
        String stateName = getMainStateName(role.getStateSupport().getStateName());
        
        BudRegistry.getInstance().register(owner, bud, budNPCData, stateName);
        System.out.println("[BUD] Now tracking Bud for player " + ownerId + " - Initial state: " + stateName);

        // Start polling when at least one Bud is tracked
        startPolling();
    }
    
    /**
     * Stop tracking a Bud.
     */
    public void untrackBud(UUID ownerId) {
        Set<BudInstance> buds = BudRegistry.getInstance().getByOwner(ownerId);
        Set<BudInstance> copy = Set.copyOf(buds);
        
        for (BudInstance bud : copy) {
            BudRegistry.getInstance().unregister(bud.getEntity());
        }
        System.out.println("[BUD] Stopped tracking Bud for player " + ownerId);

        if (BudRegistry.getInstance().getAllRefs().isEmpty()) {
            stopPolling();
        }
    }
    
    public void untrackBud(BudInstance instance) {
        BudRegistry.getInstance().unregister(instance.getEntity());
        if (BudRegistry.getInstance().getAllRefs().isEmpty()) {
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
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        if (owners.isEmpty()) {
            return;
        }

        for (UUID ownerId : owners) {
            Set<BudInstance> buds = BudRegistry.getInstance().getByOwner(ownerId);
            if (buds.isEmpty()) {
                continue;
            }
            
            for (BudInstance budInstance : buds) {
                if (budInstance.getEntity() == null || budInstance.getRef() == null) {
                    continue;
                }
    
                Store<EntityStore> store = budInstance.getRef().getStore();
                if (store != null) {
                    World world = store.getExternalData().getWorld();
                    if (world != null) {
                        world.execute(() -> checkStateChange(ownerId, budInstance));
                    }
                }
            }

        }
    }

    private void checkStateChange(UUID ownerId, BudInstance budInstance) {
        NPCEntity bud = budInstance.getEntity();
        Role role = bud.getRole();
        if (role == null) {
            return;
        }

        String currentState = getMainStateName(role.getStateSupport().getStateName());
        String lastState = budInstance.getLastKnownState();

        if (lastState != null && lastState.equals(currentState)) {
            return;
        }

        budInstance.setLastKnownState(currentState);
        onStateChanged(ownerId, budInstance, lastState, currentState);
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
        // Only handle F-key (Use) interactions
        if (event.getActionType() != InteractionType.Use) {
            return;
        }
        
        Entity targetEntity = event.getTargetEntity();
        if (targetEntity == null || !(targetEntity instanceof NPCEntity)) {
            return;
        }
        
        NPCEntity targetNPC = (NPCEntity) targetEntity;
        
        Ref<EntityStore> targetRef = event.getTargetRef();
        if (targetRef == null && targetNPC.getReference() != null) {
            targetRef = targetNPC.getReference();
        }

        BudInstance instance = BudRegistry.getInstance().get(targetRef);
        if (instance == null) {
            return;
        }
        
        UUID ownerId = instance.getOwner().getUuid();
        
        // Safety check invoking player
        Ref<EntityStore> playerEntityRef = event.getPlayerRef();
        if (playerEntityRef == null) return;

        Store<EntityStore> playerStore = playerEntityRef.getStore();
        PlayerRef interactingPlayerRef = playerStore.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (interactingPlayerRef == null) return;

        if (!ownerId.equals(interactingPlayerRef.getUuid())) {
            return;
        }
        
        System.out.println("[BUD] F-KEY INTERACTION DETECTED!");
        
        Role role = targetNPC.getRole();
        if (role != null) {
            String currentState = getMainStateName(role.getStateSupport().getStateName());
            String lastState = instance.getLastKnownState();
            
            if (lastState != null && lastState.equals(currentState)) {
                return;
            }

            instance.setLastKnownState(currentState);
            onStateChanged(ownerId, instance, lastState, currentState);
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

        BudInstance instance = BudRegistry.getInstance().get(targetRef);
        if (instance == null) {
            return;
        }

        if (!instance.getOwner().getUuid().equals(interactingPlayer.getUuid())) {
            return;
        }

        Role role = targetNPC.getRole();
        if (role == null) {
            return;
        }

        String currentState = getMainStateName(role.getStateSupport().getStateName());
        String lastState = instance.getLastKnownState();

        if (lastState != null && lastState.equals(currentState)) {
            return;
        }

        instance.setLastKnownState(currentState);
        onStateChanged(instance.getOwner().getUuid(), instance, lastState, currentState);
    }
    
    /**
     * Called when a Bud's state changes.
     */
    @SuppressWarnings("TooBroadCatch")
    private void onStateChanged(UUID ownerId, BudInstance budInstance, String fromState, String toState) {
        PlayerRef owner = budInstance.getOwner();
        NPCEntity bud = budInstance.getEntity();
        IBudNPCData budNPCData = budInstance.getData();
        
        System.out.println("[BUD] State changed: " + fromState + " -> " + toState);
        
        final Ref<EntityStore> ownerRef = owner.getReference();
        final World world = ownerRef != null ? ownerRef.getStore().getExternalData().getWorld() : null;
        if (world == null) {
            return;
        }
        
        IBudNPCSoundData npcSoundData = budNPCData.getBudNPCSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState(toState);
            this.soundInteraction.playSound(world, bud, soundEventID);
        }
        
        // Get prompt for the new state
        ILLMBudNPCMessage npcMessage = budNPCData.getLLMBudNPCMessage();
        if (npcMessage == null) return;

        String prompt = npcMessage.getPromptForState(toState);
        
        if (budLLM != null && prompt != null && budLLM.isEnabled()) {
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
            String fallbackMessage = npcMessage.getFallbackMessage(toState);
            this.chatInteraction.sendChatMessage(world, owner, fallbackMessage);
        }
    }

    public void triggerRandomChats() {
        if (budLLM == null || !budLLM.isEnabled()) {
            return;
        }
        
        // Iterate over all track owners
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        for (UUID ownerId : owners) {
            // Pick a random bud for this owner from the registry
            List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
            if (ownerBuds.isEmpty()) continue;
            
            BudInstance randomInstance = ownerBuds.get((int) (Math.random() * ownerBuds.size()));
            if (randomInstance == null) continue;

            triggerRandomChatForBud(randomInstance);
        }
    }

    private void triggerRandomChatForBud(BudInstance instance) {
        PlayerRef owner = instance.getOwner();
        NPCEntity bud = instance.getEntity();
        IBudNPCData budNPCData = instance.getData();

        if (budNPCData == null) return;
        
        ILLMBudNPCMessage npcMessage = budNPCData.getLLMBudNPCMessage();
        String npcName = npcMessage.getNPCName();
        System.out.println("[BUD] current bud: " + npcName);
        
        System.out.println("[BUD] Start extracting world data.");
        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null) return;
        
        Store<EntityStore> store = ownerRef.getStore();
        World world = store.getExternalData().getWorld();
        BudWorldContext context = getWorldContext(owner, world, store);
        System.err.println("[BUD] World data extracted: " + context.toString());

        IBudNPCSoundData npcSoundData = budNPCData.getBudNPCSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState("PetPassive");
            this.soundInteraction.playSound(world, bud, soundEventID);
        }

        String prompt = LLMWorldInfoMessageManager.createPrompt(context, npcMessage);
        
        Thread.ofVirtual().start(() -> {
            try {
                String response = budLLM.callLLM(prompt);
                String message = npcName + ": " + response;
                this.chatInteraction.sendChatMessage(world, owner, message);
            } catch (Exception e) {
                System.out.println("[BUD] Random Chat Error: " + e.getMessage());
            }
        });
    }

    private BudWorldContext getWorldContext(PlayerRef owner, World world, Store<EntityStore> store) {
        Vector3d pos = owner.getTransform().getPosition();
        TimeOfDay timeOfDay = BudTimeInformation.getTimeOfDay(store);
        System.out.println("[BUD] time of day: " + timeOfDay.name());
        Biome currentBiome = BudWorldInformation.getCurrentBiome(world, pos);
        System.out.println("[BUD] current biome: " + currentBiome.getName());
        Zone currentZone = BudWorldInformation.getCurrentZone(world, pos);
        System.out.println("[BUD] current zone: " + currentZone.name());
        return new BudWorldContext(timeOfDay, currentZone, currentBiome);
    }
}
