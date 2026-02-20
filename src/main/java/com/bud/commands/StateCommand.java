package com.bud.commands;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.interaction.ChatInteraction;
import com.bud.npc.BudManager;
import com.bud.npc.creation.BudCreation;
import com.bud.reaction.state.BudState;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
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

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

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
            IResult stateResult = changeState(playerRef, store, BudState.PET_DEFENSIVE);
            if (stateResult.isSuccess()) {
                this.chatInteraction.sendChatMessage(world, playerRef, stateResult.getMessage());
            }

        } else if (this.passiveFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to passive mode for player "
                            + playerRef.getUsername());
            IResult stateResult = changeState(playerRef, store, BudState.PET_PASSIVE);
            if (stateResult.isSuccess()) {
                this.chatInteraction.sendChatMessage(world, playerRef, stateResult.getMessage());
            }

        } else if (this.sittingFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to sitting mode for player "
                            + playerRef.getUsername());
            IResult stateResult = changeState(playerRef, store, BudState.PET_SITTING);
            if (stateResult.isSuccess()) {
                this.chatInteraction.sendChatMessage(world, playerRef, stateResult.getMessage());
            }

        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Changing Bud state to next state for player " + playerRef.getUsername());
            // TODO: Change to "next" state (def -> passive -> sit -> def)
        }
    }

    // TODO: Move to other class
    private IResult changeState(PlayerRef playerRef, Store<EntityStore> store, BudState petState) {
        Set<NPCEntity> buds = BudManager.getInstance().getOwnedBuds(playerRef.getUuid(), store);
        boolean successed = false;
        for (NPCEntity bud : buds) {
            IResult result = BudCreation.changeRoleState(bud, playerRef, petState);
            if (result.isSuccess()) {
                successed = true;
            }
        }
        if (successed) {
            String state = petState.getStateName().replace("Pet", "");
            return new SuccessResult("Changed bud state to " + state + ".");
        } else {
            return new SuccessResult("No role changed.");
        }
    }

}
