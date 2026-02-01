package com.bud.npc;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bud.system.BudTimeInformation;
import com.bud.system.BudWorldContext;
import com.bud.system.BudWorldInformation;
import com.bud.system.TimeOfDay;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

import com.bud.interaction.BudChatInteraction;
import com.bud.interaction.BudSoundInteraction;
import com.bud.npcsound.IBudNPCSoundData;
import com.bud.llm.BudLLM;
import com.bud.llmmessage.ILLMBudNPCMessage;
import com.bud.llmworldmessage.LLMWorldInfoMessageManager;
import com.bud.npcdata.IBudNPCData;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

/**
 * Tracks Bud NPC interactions and sends LLM-generated chat messages when F-key
 * is pressed.
 * Listens to PlayerInteractEvent with InteractionType.Use (F-key).
 * States: PetDefensive, PetPassive, PetSitting
 */
public class NPCStateTracker {

    private static final NPCStateTracker INSTANCE = new NPCStateTracker();

    private NPCStateTracker() {
    }

    private ScheduledFuture<?> pollingTask;

    private final BudLLM budLLM = new BudLLM();

    private final BudChatInteraction chatInteraction = new BudChatInteraction();

    private final BudSoundInteraction soundInteraction = new BudSoundInteraction();

    public static NPCStateTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Start tracking a Bud for state changes.
     */
    public IResult registerBud(PlayerRef owner, NPCEntity bud, IBudNPCData budNPCData) {
        Ref<EntityStore> budRef = bud.getReference();
        if (budRef == null) {
            return new ErrorResult("Bud NPC has no valid reference");
        }
        Role role = bud.getRole();
        if (role == null) {
            return new ErrorResult("Bud NPC has no valid Role");
        }
        BudRegistry.getInstance().register(owner, bud, budNPCData,
                getMainStateName(role.getStateSupport().getStateName()));

        // Start polling when at least one Bud is tracked
        startPolling();
        return new SuccessResult("Bud registered for tracking for player " + owner.getUuid());
    }

    public IResult unregisterBud(NPCEntity bud) {
        try {
            BudRegistry.getInstance().unregister(bud);
            if (BudRegistry.getInstance().getAllRefs().isEmpty()) {
                stopPolling();
            }
            return new SuccessResult("Stopped tracking for bud " + bud.getUuid());
        } catch (Exception e) {
            return new ErrorResult("Error untracking Bud: " + e.getMessage());
        }
    }

    public synchronized void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::pollStates, 200L, 200L,
                TimeUnit.MILLISECONDS);
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
        if (npcMessage == null)
            return;

        String prompt = npcMessage.getPromptForState(toState);

        if (budLLM != null && prompt != null && budLLM.isEnabled()) {
            Thread.ofVirtual().start(() -> {
                String message;
                try {
                    String response = budLLM.callLLM(prompt);
                    message = budNPCData.getNPCDisplayName() + ": " + response;
                } catch (java.io.IOException | InterruptedException e) {
                    System.out.println("[BUD] LLM error: " + e.getMessage());
                    String fallbackMessage = npcMessage.getFallbackMessage(toState);
                    message = budNPCData.getNPCDisplayName() + ": " + fallbackMessage;
                }
                this.chatInteraction.sendChatMessage(world, owner, message);
            });
        } else {
            String fallbackMessage = npcMessage.getFallbackMessage(toState);
            this.chatInteraction.sendChatMessage(world, owner, fallbackMessage);
        }
    }

    public IResult triggerRandomChats() {
        // Iterate over all track owners
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        for (UUID ownerId : owners) {
            // Pick a random bud for this owner from the registry
            List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
            if (ownerBuds.isEmpty())
                continue;

            BudInstance randomInstance = ownerBuds.get((int) (Math.random() * ownerBuds.size()));
            if (randomInstance == null)
                continue;

            triggerRandomChatForBud(randomInstance);
        }
        return new SuccessResult("Triggered random chats for all bud owners.");
    }

    private void triggerRandomChatForBud(BudInstance instance) {
        PlayerRef owner = instance.getOwner();
        NPCEntity bud = instance.getEntity();
        IBudNPCData budNPCData = instance.getData();

        if (budNPCData == null)
            return;

        ILLMBudNPCMessage npcMessage = budNPCData.getLLMBudNPCMessage();
        String npcName = budNPCData.getNPCDisplayName();
        System.out.println("[BUD] current bud: " + npcName);

        System.out.println("[BUD] Start extracting world data.");
        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null)
            return;

        Store<EntityStore> store = ownerRef.getStore();
        World world = store.getExternalData().getWorld();
        BudWorldContext context = getWorldContext(owner, world, store);
        System.out.println("[BUD] World data extracted: " + context.currentBiome().getName() + ", "
                + context.currentZone().name() + ", " + context.timeOfDay().name());

        System.out.println("[BUD] Preparing Sound.");
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
                System.out.println("[BUD] LLM response: " + message);
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

    /**
     * Extract main state name from full state string (e.g., "PetDefensive.Default"
     * -> "PetDefensive")
     */
    private String getMainStateName(String fullStateName) {
        if (fullStateName == null)
            return "Unknown";
        int dotIndex = fullStateName.indexOf('.');
        return dotIndex > 0 ? fullStateName.substring(0, dotIndex) : fullStateName;
    }
}
