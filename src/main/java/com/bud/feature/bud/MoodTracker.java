package com.bud.feature.bud;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.config.ReactionConfig;
import com.bud.core.types.DayOfWeek;
import com.bud.core.types.Mood;
import com.bud.feature.AbstractTracker;
import com.bud.feature.LLMInteractionManager;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.world.WorldResolver;
import com.bud.feature.world.time.TimeInformationUtil;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.profiles.IBudProfile;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.World;

public class MoodTracker extends AbstractTracker {

    private static final MoodTracker INSTANCE = new MoodTracker();

    private final LLMInteractionManager interactionManager = LLMInteractionManager.getInstance();

    private MoodTracker() {
    }

    private DayOfWeek lastPollDay;

    public static MoodTracker getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling()) {
            return;
        }
        long interval = ReactionConfig.getInstance().getMoodReactionPeriod();
        lastPollDay = TimeInformationUtil.getDayOfWeek();
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::changeMood, interval, interval, TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] Mood tracker started.");
    }

    private void changeMood() {
        World world = WorldResolver.resolveDefaultWorld().orElse(null);
        if (world == null) {
            return;
        }
        Set<BudComponent> allBuds = BudManager.getInstance().getAllBuds(world);
        if (allBuds.isEmpty()) {
            return;
        }

        boolean isDayTransition = false;
        DayOfWeek currentPollDay = TimeInformationUtil.getDayOfWeek(world);
        if (currentPollDay == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Could not determine current day of week. Skipping mood change.");
            return;
        }
        if (!lastPollDay.equals(currentPollDay)) {
            isDayTransition = true;
            lastPollDay = currentPollDay;
        }

        for (BudComponent budComponent : allBuds) {
            changeBudMood(budComponent, currentPollDay, isDayTransition);
        }
    }

    private void changeBudMood(BudComponent budComponent, DayOfWeek currentPollDay,
            boolean isDayTransition) {
        IBudProfile budProfile = BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
        DayOfWeek favDay = budProfile.getFavoriteDay();
        LoggerUtil.getLogger().info(() -> "[BUD] Checking mood for " + budProfile.getNPCDisplayName()
                + ". Current day: " + currentPollDay + ", Favorite day: " + favDay);

        if (currentPollDay.equals(favDay)) {
            if (isDayTransition) {
                budComponent.setCurrentMood(Mood.OVERMOTIVATED);
                LoggerUtil.getLogger().info(() -> "[BUD] Favorite day transition detected for "
                        + budProfile.getNPCDisplayName() + ". Ready for interaction.");
                LLMInteractionEntry interactionEntry = new LLMInteractionEntry(new LLMFavoriteDayMessageCreation(),
                        new FavoriteDayEntry(budComponent));
                Thread.ofVirtual().start(() -> {
                    interactionManager.processInteraction(interactionEntry);
                });
            } else if (!budComponent.getCurrentMood().equals(Mood.OVERMOTIVATED)) {
                budComponent.setCurrentMood(Mood.OVERMOTIVATED);
                LoggerUtil.getLogger().info(() -> "[BUD] Favorite day detected for "
                        + budProfile.getNPCDisplayName() + ". Mood set to OVERMOTIVATED.");
            }
        } else {
            if (budComponent.getCurrentMood().equals(Mood.DEFAULT)) {
                if (Math.random() < 0.5) {
                    budComponent.setCurrentMood(Mood.getRandomMood());
                    LoggerUtil.getLogger().info(() -> "[BUD] Random mood change for "
                            + budProfile.getNPCDisplayName() + ": " + budComponent.getCurrentMood());
                    if (DebugConfig.getInstance().isEnableMoodChangeDebugInfo()) {
                        ChatEvent.dispatch(budComponent.getPlayerRef(),
                                "Mood of " + budProfile.getNPCDisplayName() + " has changed to "
                                        + budComponent.getCurrentMood() + "!");
                    }
                }
            } else {
                budComponent.setCurrentMood(Mood.DEFAULT);
                LoggerUtil.getLogger().info(() -> "[BUD] Mood reset to DEFAULT for "
                        + budProfile.getNPCDisplayName());
                if (DebugConfig.getInstance().isEnableMoodChangeDebugInfo()) {
                    ChatEvent.dispatch(budComponent.getPlayerRef(),
                            "Mood of " + budProfile.getNPCDisplayName() + " has changed to "
                                    + budComponent.getCurrentMood() + "!");
                }
            }
        }
    }
}