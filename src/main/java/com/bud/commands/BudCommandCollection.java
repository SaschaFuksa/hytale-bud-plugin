package com.bud.commands;

import javax.annotation.Nonnull;

import com.bud.RegistryManager;
import com.bud.interaction.ChatInteraction;
import com.bud.npc.BudManager;
import com.bud.npc.creation.BudCreation;
import com.bud.result.IDataListResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudCommandCollection extends AbstractCommandCollection {

    public BudCommandCollection() {
        super("bud", "Commands for managing Buds");
        this.addUsageVariant(new BudCommand());
        this.addSubCommand(new CreationCommand());
        this.addSubCommand(new DeletionCommand());
        this.addSubCommand(new StateCommands());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    private static class BudCommand extends AbstractPlayerCommand {

        private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

        public BudCommand() {
            super("bud", "Spawn Buds.");
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating all Buds for player " + playerRef.getUsername());
            createAllBuds(playerRef, store, world);
        }

        // TODO: MOVE TO OTHER CLASS
        private void createAllBuds(PlayerRef playerRef, Store<EntityStore> store, World world) {
            IDataListResult<NPCEntity> teleportResult = BudManager.getInstance().teleportBuds(playerRef, store);
            if (teleportResult.isSuccess()) {
                this.chatInteraction.sendChatMessage(world, playerRef, teleportResult.getMessage());
            } else {
                teleportResult.printResult();
            }
            IDataListResult<NPCEntity> creationResult = BudCreation.createBud(store, playerRef);
            if (creationResult.isSuccess()) {
                this.chatInteraction.sendChatMessage(world, playerRef, creationResult.getMessage());
            } else {
                creationResult.printResult();
            }
            RegistryManager.getInstance().registerPlayer(playerRef);
        }
    }

}
