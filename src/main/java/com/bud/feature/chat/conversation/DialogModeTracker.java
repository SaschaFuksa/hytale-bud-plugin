package com.bud.feature.chat.conversation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ConversationConfig;
import com.bud.feature.AbstractTracker;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.profiles.IBudProfile;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DialogModeTracker extends AbstractTracker {

    @Nonnull
    private static final DialogModeTracker INSTANCE = new DialogModeTracker();

    private final Map<String, DialogSessionState> sessions = new ConcurrentHashMap<>();

    private DialogModeTracker() {
    }

    @Nonnull
    public static DialogModeTracker getInstance() {
        return Objects.requireNonNull(INSTANCE);
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling() || !ConversationConfig.getInstance().isEnableDialogMode()) {
            return;
        }
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::tickDialogMode,
                1L, 1L, TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] Dialog mode tracker started.");
    }

    public void clearPlayer(@Nonnull String playerName) {
        this.sessions.remove(playerName.toLowerCase());
    }

    public void onDialogInteractionCompleted(@Nonnull ConversationContext context, @Nonnull IBudProfile budProfile,
            String message) {
        if (context.getConversationMode() != ConversationMode.DIALOG_MODE) {
            return;
        }

        String playerName = context.getConversationOwnerKey().toLowerCase();
        DialogSessionState state = this.sessions.get(playerName);
        if (state == null) {
            return;
        }

        synchronized (state) {
            state.awaitingResponse = false;
            state.nextTurnAt = System.currentTimeMillis()
                    + TimeUnit.SECONDS.toMillis(ConversationConfig.getInstance().getDialogModeTurnIntervalSeconds());
            if (message != null && !message.isBlank()) {
                state.lastSpeakerName = budProfile.getNPCDisplayName();
                state.lastMessage = message.trim();
            }
        }
    }

    public boolean triggerDialogNow(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Store<EntityStore> entityStore = playerEntityRef.getStore();
        World world = entityStore.getExternalData().getWorld();
        world.execute(() -> {
            try {
                String playerName = playerRef.getUsername().toLowerCase();
                DialogSessionState state = this.sessions.computeIfAbsent(playerName,
                        ignored -> new DialogSessionState());
                synchronized (state) {
                    state.start(System.currentTimeMillis());
                }
                future.complete(processPlayer(playerEntityRef, playerRef));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        try {
            return future.join();
        } catch (Exception exception) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Could not trigger dialog mode for player "
                    + playerRef.getUsername() + ": " + exception.getMessage());
            return false;
        }
    }

    private void tickDialogMode() {
        if (!ConversationConfig.getInstance().isEnableDialogMode()) {
            return;
        }

        for (PlayerRef playerRef : BudManager.getInstance().getTrackedPlayers()) {
            if (!playerRef.isValid()) {
                clearPlayer(playerRef.getUsername());
                continue;
            }

            Ref<EntityStore> playerEntityRef = playerRef.getReference();
            if (playerEntityRef == null) {
                continue;
            }

            Store<EntityStore> entityStore = playerEntityRef.getStore();
            World world = entityStore.getExternalData().getWorld();
            world.execute(() -> processPlayer(playerEntityRef, playerRef));
        }
    }

    private boolean processPlayer(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef) {
        Store<EntityStore> entityStore = playerEntityRef.getStore();
        PlayerBudComponent playerComponent = entityStore.getComponent(playerEntityRef,
                PlayerBudComponent.getComponentType());
        if (playerComponent == null || !playerComponent.hasBuds()) {
            return false;
        }

        List<BudComponent> budComponents = new ArrayList<>();
        playerComponent.getCurrentBuds().forEach(bud -> {
            BudComponent budComponent = BudManager.getInstance().findBudComponent(bud);
            if (budComponent != null) {
                budComponents.add(budComponent);
            }
        });
        if (budComponents.size() < 2) {
            return false;
        }

        long now = System.currentTimeMillis();
        String playerName = playerRef.getUsername().toLowerCase();
        DialogSessionState state = this.sessions.computeIfAbsent(playerName,
                ignored -> new DialogSessionState());

        synchronized (state) {
            if (!state.active) {
                long lastMessageAt = Orchestrator.getInstance().getLastGlobalMessageTime(playerRef.getUsername());
                long idleMillis = TimeUnit.SECONDS.toMillis(ConversationConfig.getInstance().getDialogModeIdleSeconds());
                if (now - lastMessageAt >= idleMillis) {
                    state.start(now);
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Dialog mode activated for player " + playerRef.getUsername()
                                    + " after " + (now - lastMessageAt) + "ms of silence.");
                }
            }

            if (!state.active) {
                return false;
            }

            if (now >= state.activeUntil) {
                state.finish(now);
                LoggerUtil.getLogger().fine(() -> "[BUD] Dialog mode ended for player " + playerRef.getUsername());
                return false;
            }

            if (state.awaitingResponse || now < state.nextTurnAt) {
                return false;
            }

            BudComponent nextSpeaker = pickNextSpeaker(budComponents, state.lastSpeakerName);
            if (nextSpeaker == null) {
                return false;
            }

            Set<String> participants = buildParticipants(playerRef.getUsername(), budComponents);
            DialogEntry entry = new DialogEntry(playerRef.getUsername(), participants, state.lastSpeakerName,
                    state.lastMessage, nextSpeaker);
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.SOCIAL,
                    entry,
                    "dialogMode:" + now,
                    playerRef.getUsername(),
                    new LLMInteractionEntry(LLMDialogMessageCreation.getInstance(), entry),
                    now));
            state.awaitingResponse = true;
            return true;
        }
    }

    private BudComponent pickNextSpeaker(@Nonnull List<BudComponent> budComponents, String lastSpeakerName) {
        if (budComponents.isEmpty()) {
            return null;
        }
        for (BudComponent budComponent : budComponents) {
            String budName = BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType())
                    .getNPCDisplayName();
            if (lastSpeakerName == null || !lastSpeakerName.equalsIgnoreCase(budName)) {
                return budComponent;
            }
        }
        return budComponents.get(0);
    }

    @Nonnull
    private Set<String> buildParticipants(@Nonnull String playerName, @Nonnull List<BudComponent> budComponents) {
        Set<String> participants = new LinkedHashSet<>();
        participants.add(playerName);
        for (BudComponent budComponent : budComponents) {
            participants.add(BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType())
                    .getNPCDisplayName());
        }
        return participants;
    }

    private static class DialogSessionState {

        private boolean active;
        private boolean awaitingResponse;
        private long activeUntil;
        private long nextTurnAt;
        private String lastSpeakerName;
        private String lastMessage;

        private void start(long now) {
            this.active = true;
            this.awaitingResponse = false;
            this.activeUntil = now
                    + TimeUnit.SECONDS.toMillis(ConversationConfig.getInstance().getDialogModeActiveSeconds());
            this.nextTurnAt = now;
            this.lastSpeakerName = null;
            this.lastMessage = null;
        }

        private void finish(long now) {
            this.active = false;
            this.awaitingResponse = false;
            this.activeUntil = 0L;
            this.nextTurnAt = 0L;
            this.lastSpeakerName = null;
            this.lastMessage = null;
        }
    }
}