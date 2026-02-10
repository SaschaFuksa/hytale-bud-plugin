package com.bud.npc;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.interaction.InteractionManager;
import com.bud.reaction.world.time.Mood;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class BudMoodTracker {

    private static final BudMoodTracker INSTANCE = new BudMoodTracker();

    private final InteractionManager interactionManager = InteractionManager.getInstance();

    private BudMoodTracker() {
    }

    private volatile ScheduledFuture<?> pollingTask;

    public static BudMoodTracker getInstance() {
        return INSTANCE;
    }

    public synchronized void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        long interval = BudConfig.getInstance().getMoodReactionPeriod();
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::changeMood), 3L, interval,
                TimeUnit.MINUTES);
        LoggerUtil.getLogger().info(() -> "[BUD] Mood tracker started.");
    }

    public synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
            LoggerUtil.getLogger().fine(() -> "[BUD] Stopped mood polling task");
        }
    }

    private void changeMood() {
        BudRegistry budRegistry = BudRegistry.getInstance();
        Set<UUID> owners = budRegistry.getAllOwners();
        if (owners.isEmpty()) {
            return;
        }
        for (UUID owner : owners) {
            Set<BudInstance> bud = budRegistry.getByOwner(owner);
            for (BudInstance instance : bud) {
                changeMood(instance);
            }
        }
    }

    private void changeMood(BudInstance budInstance) {
        if (!budInstance.getCurrentMood().equals(Mood.DEFAULT) || Math.random() < 0.5) {
            budInstance.setCurrentMood(Mood.DEFAULT);
        } else {
            Mood randomMood = Mood.getRandomMood();
            budInstance.setCurrentMood(randomMood);
        }
    }
}