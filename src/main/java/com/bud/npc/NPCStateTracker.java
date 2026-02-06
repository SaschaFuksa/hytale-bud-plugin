package com.bud.npc;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import com.bud.BudConfig;
import com.bud.interaction.BudChatInteraction;
import com.bud.interaction.BudSoundInteraction;
import com.bud.npc.npcdata.IBudNPCData;
import com.bud.npc.npcsound.IBudNPCSoundData;
import com.bud.llm.llmmessage.BudLLMMessage;
import com.bud.llm.llmclient.ILLMClient;
import com.bud.llm.llmclient.LLMClientFactory;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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

    private volatile ScheduledFuture<?> pollingTask;

    private ILLMClient llmClient;

    private final BudChatInteraction chatInteraction = BudChatInteraction.getInstance();

    private final BudSoundInteraction soundInteraction = BudSoundInteraction.getInstance();

    private ILLMClient getLlmClient() {
        if (llmClient == null) {
            llmClient = LLMClientFactory.createClient();
        }
        return llmClient;
    }

    private boolean isEnableLLM() {
        return BudConfig.getInstance().isEnableLLM();
    }

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
        LoggerUtil.getLogger().fine(() -> "[BUD] Started state polling task");
    }

    public synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
            LoggerUtil.getLogger().fine(() -> "[BUD] Stopped state polling task");
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

        LoggerUtil.getLogger().fine(() -> "[BUD] State changed: " + fromState + " -> " + toState);

        final Ref<EntityStore> ownerRef;
        ownerRef = owner.getReference();
        final World world = ownerRef != null ? ownerRef.getStore().getExternalData().getWorld() : null;
        if (world == null) {
            return;
        }

        // Get prompt for the new state
        BudLLMMessage npcMessage = budNPCData.getLLMBudNPCMessage();
        if (npcMessage == null)
            return;

        String prompt = npcMessage.getState(toState);

        if (isEnableLLM() && getLlmClient() != null && prompt != null) {
            getLlmClient().callLLMAsync(
                    prompt,
                    response -> {
                        String message = budNPCData.getNPCDisplayName() + ": " + response;
                        this.chatInteraction.sendChatMessage(world, owner, message);
                    },
                    error -> {
                        String fallbackMessage = npcMessage.getFallback(toState);
                        String message = budNPCData.getNPCDisplayName() + ": " + fallbackMessage;
                        this.chatInteraction.sendChatMessage(world, owner, message);
                    });
        } else {
            String fallbackMessage = npcMessage.getFallback(toState);
            LoggerUtil.getLogger().fine(() -> "[BUD] Sending fallback message: " + fallbackMessage);
            this.chatInteraction.sendChatMessage(world, owner, fallbackMessage);
        }

        IBudNPCSoundData npcSoundData = budNPCData.getBudNPCSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState(toState);
            this.soundInteraction.playSound(world, bud, soundEventID);
        }
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
