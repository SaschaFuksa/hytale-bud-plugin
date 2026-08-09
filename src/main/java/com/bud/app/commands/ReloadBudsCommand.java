package com.bud.app.commands;

import javax.annotation.Nonnull;

import com.bud.core.registry.BudRegistry;
import com.bud.feature.chat.ChatEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ReloadBudsCommand extends AbstractPlayerCommand {

    private final FlagArg resetFlag;

    public ReloadBudsCommand() {
        super("buds", "Manage Bud definitions (buds/*.yml, roster.yml).");
        this.resetFlag = this.withFlagArg("reset", "Reset Bud definitions to default.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.resetFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Resetting Bud definitions by player: " + playerRef.getUsername());
            BudRegistry.getInstance().reset();
            ChatEvent.dispatch(playerRef, "Reset Bud definitions to default.");
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Reloading Bud definitions by player: " + playerRef.getUsername());
            BudRegistry.getInstance().reloadMissing();
            ChatEvent.dispatch(playerRef, "Reloaded Bud definitions.");
        }
    }
}
