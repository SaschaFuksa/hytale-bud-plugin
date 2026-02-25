package com.bud.app.commands;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.BudState;
import com.bud.feature.queue.state.StateChangeEntry;
import com.bud.feature.queue.state.StateChangeQueue;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.old.BudManager;
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

    public StateCommand() {
        super("state", "Commands for checking and managing Bud state.");
        this.defensiveFlag = this.withFlagArg("defensive", "Change Bud state to defensive mode.");
        this.passiveFlag = this.withFlagArg("passive", "Change Bud state to passive mode.");
        this.sittingFlag = this.withFlagArg("sitting", "Change Bud state to sitting mode.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.defensiveFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to defensive mode for player "
                            + playerRef.getUsername());
            this.changeState(ref, store, BudState.PET_DEFENSIVE);
        } else if (this.passiveFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to passive mode for player "
                            + playerRef.getUsername());
            this.changeState(ref, store, BudState.PET_PASSIVE);
        } else if (this.sittingFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to sitting mode for player "
                            + playerRef.getUsername());
            this.changeState(ref, store, BudState.PET_SITTING);
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to next state for player " + playerRef.getUsername());
            this.changeState(ref, store, null);
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
            if (newState == null) {
                newState = BudManager.getInstance().getNextState(budComponent.getCurrentState());
                StateChangeQueue.getInstance()
                        .addToCache(new StateChangeEntry(newState, budComponent));
            } else {
                StateChangeQueue.getInstance()
                        .addToCache(new StateChangeEntry(newState, budComponent));
            }
        }
    }

}
