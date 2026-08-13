package com.bud.app.commands;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.types.BudState;
import com.bud.feature.queue.state.StateChangeEntry;
import com.bud.feature.queue.state.StateChangeQueue;
import com.bud.feature.state.StateChangeEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class StateCommand extends AbstractPlayerCommand {

    private final FlagArg defensiveFlag;

    private final FlagArg passiveFlag;

    private final FlagArg sittingFlag;

    private final FlagArg workingFlag;

    public StateCommand() {
        super("state", "Commands for checking and managing Bud state.");
        defensiveFlag = withFlagArg("defensive", "Change Bud state to defensive mode.");
        passiveFlag = withFlagArg("passive", "Change Bud state to passive mode.");
        sittingFlag = withFlagArg("sitting", "Change Bud state to sitting mode.");
        workingFlag = withFlagArg("working",
                "Debug (disabled by default, see DebugConfig.EnableWorkingStateDebugCommand): change Bud state "
                        + "to working mode without a Workstation.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (defensiveFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to defensive mode for player "
                            + playerRef.getUsername());
            changeState(ref, store, BudState.PET_DEFENSIVE);
        } else if (passiveFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to passive mode for player "
                            + playerRef.getUsername());
            changeState(ref, store, BudState.PET_PASSIVE);
        } else if (sittingFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to sitting mode for player "
                            + playerRef.getUsername());
            changeState(ref, store, BudState.PET_SITTING);
        } else if (workingFlag.get(context)) {
            if (!DebugConfig.getInstance().isEnableWorkingStateDebugCommand()) {
                LoggerUtil.getLogger()
                        .warning(
                                () -> "[BUD] --working is disabled (see DebugConfig.EnableWorkingStateDebugCommand) for "
                                        + playerRef.getUsername());
                return;
            }
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to working mode (debug) for player "
                            + playerRef.getUsername());
            setWorkingSilently(ref, store, playerRef);
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to next state for player " + playerRef.getUsername());
            changeState(ref, store, null);
        }
    }

    private void changeState(@Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store, BudState newState) {
        PlayerBudComponent playerComponent = store.getComponent(ref, PlayerBudComponent.getComponentType());
        for (NPCEntity bud : playerComponent.getCurrentBuds()) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null || !budRef.isValid()) {
                continue;
            }
            BudComponent budComponent = store.getComponent(budRef, BudComponent.getComponentType());
            if (budComponent == null) {
                continue;
            }
            BudState targetState = newState;
            if (targetState == null) {
                targetState = BudManager.getInstance().getNextState(budComponent.getCurrentState());
            }
            final BudState resolvedTargetState = targetState;
            if (resolvedTargetState == budComponent.getCurrentState()) {
                LoggerUtil.getLogger()
                        .fine(() -> "[BUD] Skipping state change for NPC \""
                                + budComponent.getBud().getNPCTypeId()
                                + "\" because it is already in state "
                                + resolvedTargetState.getStateName());
                continue;
            }
            StateChangeQueue.getInstance()
                    .addToCache(new StateChangeEntry(resolvedTargetState, budComponent));
        }
    }

    private void setWorkingSilently(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef) {
        PlayerBudComponent playerComponent = store.getComponent(ref, PlayerBudComponent.getComponentType());
        for (NPCEntity bud : playerComponent.getCurrentBuds()) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null || !budRef.isValid()) {
                continue;
            }
            BudComponent budComponent = store.getComponent(budRef, BudComponent.getComponentType());
            if (budComponent == null || budComponent.getCurrentState() == BudState.WORKING) {
                continue;
            }
            budComponent.setCurrentState(BudState.WORKING);
            StateChangeEvent.dispatch(budComponent.getBud(), playerRef, BudState.WORKING);
        }
    }

}
