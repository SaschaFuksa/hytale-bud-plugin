package com.bud.commands;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.RegistryManager;
import com.bud.events.BudCreationEvent;
import com.bud.interaction.ChatInteraction;
import com.bud.npc.BudManager;
import com.bud.npc.buds.BudType;
import com.bud.npc.buds.GronkhData;
import com.bud.npc.buds.IBudData;
import com.bud.npc.buds.KeylethData;
import com.bud.npc.creation.BudCreation;
import com.bud.player.PlayerRegistry;
import com.bud.result.IDataListResult;
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
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class CreationCommand extends AbstractPlayerCommand {

    private final FlagArg allFlag;

    private final FlagArg veriFlag;

    private final FlagArg keylethFlag;

    private final FlagArg gronkhFlag;

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    public CreationCommand() {
        super("create", "Bud creation commands.");
        this.allFlag = this.withFlagArg("all", "Create all Buds.");
        this.veriFlag = this.withFlagArg("veri", "Create Veri Bud.");
        this.keylethFlag = this.withFlagArg("keyleth", "Create Keyleth Bud.");
        this.gronkhFlag = this.withFlagArg("gronkh", "Create Gronkh Bud.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.allFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating all Buds for player " + playerRef.getUsername());
            createAllBuds(playerRef, store, world);
        } else if (this.veriFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Veri Bud for player " + playerRef.getUsername());
            BudCreationEvent.dispatch(playerRef.getReference(), Set.of(BudType.VERI));
        } else if (this.keylethFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Keyleth Bud for player " + playerRef.getUsername());
            IResult result = executeBudAction(playerRef, store, new KeylethData());
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
        } else if (this.gronkhFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Gronkh Bud for player " + playerRef.getUsername());
            IResult result = executeBudAction(playerRef, store, new GronkhData());
            result.printResult();
            this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating all Buds for player " + playerRef.getUsername());
            createAllBuds(playerRef, store, world);
        }
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

    // TODO: MOVE TO OTHER CLASS
    public static IResult executeBudAction(PlayerRef playerRef, Store<EntityStore> store,
            IBudData missingBud) {
        if (BudManager.getInstance().canBeAdded(playerRef.getUuid(), store,
                missingBud)) {
            // Create new Bud
            IResult result = BudCreation.createBud(store, playerRef, Set.of(missingBud));
            if (PlayerRegistry.getInstance().getByOwner(playerRef.getUuid()) == null) {
                RegistryManager.getInstance().registerPlayer(playerRef);
            }
            return result;
        } else {
            // Teleport existing Buds
            return BudManager.getInstance().teleportBud(playerRef, store, missingBud);
        }
    }

}
