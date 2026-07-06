package com.bud.feature.queue.state;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.BudState;
import com.bud.feature.LLMInteractionManager;
import com.bud.feature.bud.reaction.BudReactionEntry;
import com.bud.feature.bud.reaction.BudReactionKind;
import com.bud.feature.bud.reaction.LLMBudReactionMessageCreation;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.queue.AbstractQueue;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.state.LLMStateMessageCreation;
import com.bud.feature.state.StateChangeEvent;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.profiles.IBudProfile;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class StateChangeQueue extends AbstractQueue {

    private static final StateChangeQueue INSTANCE = new StateChangeQueue();

    private StateChangeQueue() {
    }

    public static StateChangeQueue getInstance() {
        return INSTANCE;
    }

    @Override
    protected void pollAndHandle() {
        StateChangeEntry entry = (StateChangeEntry) cache.poll();
        if (entry == null) {
            stopPolling();
            return;
        }
        try {
            handleStateChange(entry);
        } catch (Exception e) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Error handling state change: " + e.getMessage());
        } finally {
            if (cache.isEmpty()) {
                stopPolling();
            }
        }
    }

    private void handleStateChange(@Nonnull StateChangeEntry entry) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Handling state change: " + entry);
        BudComponent budComponent = entry.getBudComponent();
        budComponent.setCurrentState(entry.newState());
        StateChangeEvent.dispatch(budComponent.getBud(), budComponent.getPlayerRef(), entry.newState());
        Ref<EntityStore> entityRef = budComponent.getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: " + budComponent.getBud());
            return;
        }
        LLMInteractionManager.getInstance().processInteraction(
                new LLMInteractionEntry(LLMStateMessageCreation.getInstance(), entry));

        triggerStateChangeReaction(budComponent, entry.newState(), entityRef);
    }

    private void triggerStateChangeReaction(@Nonnull BudComponent budComponent, @Nonnull BudState newState,
            @Nonnull Ref<EntityStore> budEntityRef) {
        Store<EntityStore> store = budEntityRef.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            PlayerRef playerRef = budComponent.getPlayerRef();
            Ref<EntityStore> playerEntityRef = playerRef.getReference();
            if (playerEntityRef == null) {
                return;
            }
            PlayerBudComponent playerBudComponent = store.getComponent(playerEntityRef,
                    PlayerBudComponent.getComponentType());
            if (playerBudComponent == null) {
                return;
            }
            BudComponent otherBud = BudManager.getInstance().getRandomOtherBud(playerBudComponent, budComponent);
            if (otherBud == null) {
                return;
            }
            IBudProfile budProfile = BudProfileMapper.getInstance().getProfileForBudType(budComponent.getBudType());
            String stateDescription = switch (newState) {
                case PET_SITTING -> "sitting down and resting";
                case PET_PASSIVE -> "being passive and just tagging along";
                case PET_DEFENSIVE -> "back to being alert and defensive";
            };
            String situationInfo = budProfile.getNPCDisplayName() + " is now " + stateDescription
                    + ". React to this in character.";
            BudReactionEntry reactionEntry = new BudReactionEntry(otherBud, BudReactionKind.STATE_CHANGE,
                    situationInfo);
            long now = System.currentTimeMillis();
            Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                    OrchestratorChannel.SOCIAL,
                    reactionEntry,
                    reactionEntry.getEntryName() + ":" + now,
                    playerRef.getUsername(),
                    new LLMInteractionEntry(LLMBudReactionMessageCreation.getInstance(), reactionEntry),
                    now));
        });
    }

}
