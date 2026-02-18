package com.bud.commands;

import javax.annotation.Nonnull;

import com.bud.RegistryManager;
import com.bud.cleanup.CleanUpHandler;
import com.bud.interaction.ChatInteraction;
import com.bud.npc.creation.BudCreation;
import com.bud.result.IDataListResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class ResetCommand extends AbstractPlayerCommand {

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    public ResetCommand() {
        super("reset", "Reset Bud system.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Resetting Bud system for player: " + playerRef.getUsername());
        CleanUpHandler.cleanupOwnerBuds(playerRef, world).printResult();
        IDataListResult<NPCEntity> creationResult = BudCreation.createBud(store, playerRef);
        if (creationResult.isSuccess()) {
            this.chatInteraction.sendChatMessage(world, playerRef,
                    creationResult.getMessage());
        }
        RegistryManager.getInstance().registerPlayer(playerRef);
    }

}
