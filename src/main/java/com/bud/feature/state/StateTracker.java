package com.bud.feature.state;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.bud.feature.AbstractTracker;
import com.bud.feature.LLMInteractionManager;
import com.bud.old.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class StateTracker extends AbstractTracker {

    private static final StateTracker INSTANCE = new StateTracker();

    private final LLMInteractionManager interactionManager = LLMInteractionManager.getInstance();

    private StateTracker() {
    }

    public static StateTracker getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling()) {
            return;
        }
        BudRegistry budRegistry = BudRegistry.getInstance();
        if (budRegistry.getAllOwners().isEmpty()) {
            return;
        }
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::checkStates), 250L, 250L,
                TimeUnit.MILLISECONDS));
        LoggerUtil.getLogger().fine(() -> "[BUD] Started state polling task");
    }

    private void checkStates() {
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        if (owners.isEmpty()) {
            return;
        }
        // TODO
        // interactionManager.processInteraction(owners, llmStateManager);
    }

}
