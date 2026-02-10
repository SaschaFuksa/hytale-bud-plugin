package com.bud.reaction.tracker;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.interaction.InteractionManager;
import com.bud.llm.message.mood.LLMMoodManager;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.reaction.world.time.DayOfWeek;
import com.bud.reaction.world.time.Mood;
import com.bud.reaction.world.time.TimeInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class MoodTracker {

    private static final MoodTracker INSTANCE = new MoodTracker();

    private final InteractionManager interactionManager = InteractionManager.getInstance();

    private static final LLMMoodManager llmMoodManager = new LLMMoodManager();

    private MoodTracker() {
    }

    private volatile ScheduledFuture<?> pollingTask;

    private DayOfWeek lastPollDay;

    public static MoodTracker getInstance() {
        return INSTANCE;
    }

    public synchronized void startPolling() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }
        lastPollDay = TimeInformationUtil.getDayOfWeek();
        long interval = BudConfig.getInstance().getMoodReactionPeriod();
        pollingTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                () -> Thread.ofVirtual().start(this::changeMood), 3L, interval,
                TimeUnit.MINUTES);
        LoggerUtil.getLogger().info(() -> "[BUD] Mood tracker started.");
    }

    private void changeMood() {
        BudRegistry budRegistry = BudRegistry.getInstance();
        Set<UUID> owners = budRegistry.getAllOwners();
        if (owners.isEmpty()) {
            return;
        }

        boolean isDayTransition = false;
        DayOfWeek currentDay = TimeInformationUtil.getDayOfWeek();
        if (!lastPollDay.equals(currentDay)) {
            isDayTransition = true;
            lastPollDay = currentDay;
        }

        for (UUID owner : owners) {
            Set<BudInstance> buds = budRegistry.getByOwner(owner);
            for (BudInstance budInstance : buds) {
                changeMood(budInstance, currentDay, isDayTransition);
            }
        }
    }

    private void changeMood(BudInstance budInstance, DayOfWeek currentDay,
            boolean isDayTransition) {
        DayOfWeek favDay = budInstance.getData().getFavoriteDay();

        if (currentDay.equals(favDay)) {
            budInstance.setCurrentMood(Mood.OVERMOTIVATED);
            if (isDayTransition) {
                interactionManager.processInteractionForBuds(Set.of(budInstance), llmMoodManager);
                LoggerUtil.getLogger().info(() -> "[BUD] Favorite day transition detected for "
                        + budInstance.getData().getNPCTypeId() + ". Ready for interaction.");
            }
        } else {
            if (budInstance.getCurrentMood().equals(Mood.DEFAULT)) {
                if (Math.random() < 0.5) {
                    budInstance.setCurrentMood(Mood.getRandomMood());
                    LoggerUtil.getLogger().info(() -> "[BUD] Random mood change for "
                            + budInstance.getData().getNPCTypeId() + ": " + budInstance.getCurrentMood());
                }
            } else {
                budInstance.setCurrentMood(Mood.DEFAULT);
                LoggerUtil.getLogger().info(() -> "[BUD] Mood reset to DEFAULT for "
                        + budInstance.getData().getNPCTypeId());
            }
        }
    }
}