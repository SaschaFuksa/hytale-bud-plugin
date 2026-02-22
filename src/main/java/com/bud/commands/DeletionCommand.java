package com.bud.commands;

import javax.annotation.Nonnull;

import com.bud.cleanup.CleanUpHandler;
import com.bud.interaction.ChatInteraction;
import com.bud.profile.BudType;
import com.bud.result.IResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DeletionCommand extends AbstractPlayerCommand {

    private final FlagArg allFlag;

    private final FlagArg veriFlag;

    private final FlagArg keylethFlag;

    private final FlagArg gronkhFlag;

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    public DeletionCommand() {
        super("delete", "Delete Bud commands.");
        this.allFlag = this.withFlagArg("all", "Delete all Buds for all players.");
        this.veriFlag = this.withFlagArg("veri", "Delete Veri Bud.");
        this.keylethFlag = this.withFlagArg("keyleth", "Delete Keyleth Bud.");
        this.gronkhFlag = this.withFlagArg("gronkh", "Delete Gronkh Bud.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.allFlag.get(context)) {
            IResult result = CleanUpHandler.cleanupBud(playerRef, world, BudType.VERI);
            result.printResult();
            result = CleanUpHandler.cleanupBud(playerRef, world, BudType.KEYLETH);
            result.printResult();
            result = CleanUpHandler.cleanupBud(playerRef, world, BudType.GRONKH);
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
        } else if (this.veriFlag.get(context)) {
            IResult result = CleanUpHandler.cleanupBud(playerRef, world, BudType.VERI);
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
        } else if (this.keylethFlag.get(context)) {
            IResult result = CleanUpHandler.cleanupBud(playerRef, world, BudType.KEYLETH);
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
        } else if (this.gronkhFlag.get(context)) {
            IResult result = CleanUpHandler.cleanupBud(playerRef, world, BudType.GRONKH);
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
        } else {
            IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, world);
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Deleting all Buds for player "
                            + playerRef.getUsername());
        }
    }

}
