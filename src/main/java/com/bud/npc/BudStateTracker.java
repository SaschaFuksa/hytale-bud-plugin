package com.bud.npc;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import com.bud.interaction.InteractionManager;
import com.bud.npc.buds.IBudData;
import com.bud.llm.message.state.LLMStateManager;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

/**
 * Tracks Bud NPC interactions and sends LLM-generated chat messages when F-key
 * is pressed.
 * Listens to PlayerInteractEvent with InteractionType.Use (F-key).
 * States: PetDefensive, PetPassive, PetSitting
 */
public class BudStateTracker {

    private static final BudStateTracker INSTANCE = new BudStateTracker();

    private final InteractionManager interactionManager = InteractionManager.getInstance();

    private BudStateTracker() {
    }

    private volatile ScheduledFuture<?> pollingTask;

    public static BudStateTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Start tracking a Bud for state changes.
     */
    public IResult registerBud(PlayerRef owner, NPCEntity bud, IBudData budNPCData) {
        Ref<EntityStore> budRef = bud.getReference();
        if (budRef == null) {
            return new ErrorResult("Bud NPC has no valid reference");
        }
        Role role = bud.getRole();
        if (role == null) {
            return new ErrorResult("Bud NPC has no valid Role");
        }
        String mainStateName = LLMStateManager.getMainStateName(role.getStateSupport().getStateName());
        BudRegistry.getInstance().register(owner, bud, budNPCData,
                mainStateName);

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
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::pollStates, 500L, 500L,
                TimeUnit.MICROSECONDS);
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
        interactionManager.processInteraction(owners, new LLMStateManager());
    }

}
