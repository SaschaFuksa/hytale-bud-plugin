package com.bud.feature.player.state;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ReactionConfig;
import com.bud.feature.AbstractTracker;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.world.WorldResolver;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerStateTracker extends AbstractTracker {

    private static final PlayerStateTracker INSTANCE = new PlayerStateTracker();

    private PlayerStateTracker() {
    }

    public static PlayerStateTracker getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling()) {
            return;
        }
        long interval = ReactionConfig.getInstance().getPlayerStateReactionPeriodSeconds();
        setPollingTask(HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::triggerPlayerStateMessage, interval, interval,
                TimeUnit.SECONDS));
        LoggerUtil.getLogger().info(() -> "[BUD] Player state tracker started.");
    }

    private void triggerPlayerStateMessage() {
        Set<PlayerRef> players = BudManager.getInstance().getTrackedPlayers();
        if (players.isEmpty()) {
            return;
        }

        for (PlayerRef playerRef : players) {
            if (!playerRef.isValid()) {
                LoggerUtil.getLogger().warning(() -> "[BUD] Invalid player reference encountered.");
                BudManager.getInstance().unregisterPlayer(playerRef);
                continue;
            }
            World world = WorldResolver.resolveStrict(playerRef).orElse(null);
            if (world == null) {
                continue;
            }

            try {
                world.execute(() -> {
                    Ref<EntityStore> playerEntityRef = playerRef.getReference();
                    if (playerEntityRef == null) {
                        return;
                    }
                    Store<EntityStore> entityStore = playerEntityRef.getStore();
                    PlayerBudComponent playerComponent = entityStore.getComponent(playerEntityRef,
                            PlayerBudComponent.getComponentType());
                    if (playerComponent == null || !playerComponent.hasBuds()) {
                        return;
                    }

                    EffectControllerComponent effectController = entityStore.getComponent(playerEntityRef,
                            EffectControllerComponent.getComponentType());
                    if (effectController == null) {
                        return;
                    }

                    Set<String> currentEffectIds = resolveActiveEffectIds(effectController);
                    Set<String> newEffectIds = playerComponent.resolveNewEffectIds(currentEffectIds);
                    if (newEffectIds.isEmpty()) {
                        return;
                    }

                    for (String rawEffectId : newEffectIds) {
                        String effectId = Objects.requireNonNull(rawEffectId);
                        PlayerEffectClassifier.Category category = PlayerEffectClassifier.classify(effectId);
                        if (category == PlayerEffectClassifier.Category.NEUTRAL) {
                            continue;
                        }

                        BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerComponent);
                        if (budComponent == null) {
                            LoggerUtil.getLogger().fine(() -> "[BUD] No eligible (non-Working) BudComponent found "
                                    + "for player: " + playerRef.getUsername());
                            continue;
                        }

                        boolean debuff = category == PlayerEffectClassifier.Category.NEGATIVE;
                        PlayerStateEntry entry = new PlayerStateEntry(effectId, debuff, budComponent);
                        long now = System.currentTimeMillis();
                        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                                OrchestratorChannel.PLAYER,
                                entry,
                                entry.getEntryName() + ":" + now,
                                playerRef.getUsername(),
                                new LLMInteractionEntry(LLMPlayerStateMessageCreation.getInstance(), entry),
                                now));
                    }
                });
            } catch (Exception e) {
                LoggerUtil.getLogger().warning(
                        () -> "[BUD] Player state tracker could not execute on world thread: " + e.getMessage());
            }
        }
    }

    @Nonnull
    public static Set<String> resolveActiveEffectIds(EffectControllerComponent effectController) {
        Set<String> ids = new HashSet<>();
        for (int index : effectController.getActiveEffectIndexes()) {
            EntityEffect effect = EntityEffect.getAssetMap().getAsset(index);
            if (effect == null) {
                continue;
            }
            String id = effect.getId();
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
