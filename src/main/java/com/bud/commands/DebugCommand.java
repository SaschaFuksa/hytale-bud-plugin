package com.bud.commands;

import javax.annotation.Nonnull;

import com.bud.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
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
            ComponentType<EntityStore, PlayerBudComponent> componentType = PlayerBudComponent.getComponentType();
            if (componentType == null) {
                LoggerUtil.getLogger().severe(() -> "[BUD] PlayerBudComponent type is not set");
                return;
            }
            PlayerBudComponent playerBudComponent = store.getComponent(playerRefReference, componentType);
            if (playerBudComponent == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] No PlayerBudComponent found for player "
                        + playerRef.getUsername());
                return;
            }
            playerBudComponent.getBuds().forEach(bud -> {
                playerRef.sendMessage(Message.raw("- " + bud.getNPCTypeId()));
            });
            for (String loadedBud : playerBudComponent.getLoadedBuds()) {
                playerRef.sendMessage(Message.raw("- " + loadedBud));
            }

        }
    }

}
