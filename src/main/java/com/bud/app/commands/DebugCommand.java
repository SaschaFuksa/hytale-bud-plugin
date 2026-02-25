package com.bud.app.commands;

import javax.annotation.Nonnull;

import com.bud.core.components.PlayerBudComponent;
import com.bud.core.profiles.BudType;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DebugCommand extends AbstractPlayerCommand {

    private final FlagArg componentDataFlag;

    public DebugCommand() {
        super("debug", "Debug command for testing purposes.");
        this.componentDataFlag = this.withFlagArg("componentData", "Shows the current state of the player's Bud.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.componentDataFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Debug command executed for player " + playerRef.getUsername());
            Ref<EntityStore> playerRefReference = playerRef.getReference();
            if (playerRefReference == null) {
                LoggerUtil.getLogger()
                        .severe(() -> "[BUD] PlayerRef reference is null for player " + playerRef.getUsername());
                return;
            }
            PlayerBudComponent playerBudComponent = store.getComponent(playerRefReference,
                    PlayerBudComponent.getComponentType());
            if (playerBudComponent == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] No PlayerBudComponent found for player "
                        + playerRef.getUsername());
                return;
            }
            playerBudComponent.getCurrentBuds().forEach(bud -> {
                playerRef.sendMessage(Message.raw("- " + bud.getNPCTypeId()));
            });
            for (BudType budType : playerBudComponent.getBudTypes()) {
                playerRef.sendMessage(Message.raw("- " + budType.getName()));
            }

        }
    }

}
