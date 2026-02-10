package com.bud.npc;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.interaction.InteractionManager;
import com.bud.player.PlayerInstance;
import com.bud.player.PlayerRegistry;
import com.bud.reaction.world.time.DayOfWeek;
import com.bud.reaction.world.time.Mood;
import com.bud.reaction.world.time.TimeInformationUtil;
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
        PlayerRegistry playerRegistry = PlayerRegistry.getInstance();
        Set<UUID> owners = budRegistry.getAllOwners();
        if (owners.isEmpty()) {
            return;
        }
        for (UUID owner : owners) {
            Set<BudInstance> buds = budRegistry.getByOwner(owner);
            PlayerInstance playerInstance = playerRegistry.getByOwner(owner);
            for (BudInstance budInstance : buds) {
                changeMood(budInstance, playerInstance);
            }
        }
    }

    private void changeMood(BudInstance budInstance, PlayerInstance playerInstance) {
        DayOfWeek currentDay = TimeInformationUtil.getDayOfWeek(budInstance.getRef().getStore());
        DayOfWeek favDay = budInstance.getData().getFavoriteDay();
        DayOfWeek lastDay = playerInstance.getLastDay();

        if (currentDay.equals(favDay)) {
            budInstance.setCurrentMood(Mood.OVERMOTIVATED);
            if (lastDay != null && !lastDay.equals(currentDay)) {
                // TODO: Chat-Interaktion
                LoggerUtil.getLogger().info(() -> "[BUD] Favorite day transition detected for "
                        + budInstance.getData().getNPCTypeId() + ". Ready for interaction.");
            }
        } else {
            if (budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
                if (Math.random() < 0.5) {
                    budInstance.setCurrentMood(Mood.getRandomMood());
                }
            } else {
                budInstance.setCurrentMood(Mood.DEFAULT);
            }
        }

        playerInstance.setLastDay(currentDay);
    }
}