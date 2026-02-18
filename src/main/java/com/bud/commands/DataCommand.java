package com.bud.commands;

import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bud.BudPlugin;
import com.bud.interaction.ChatInteraction;
import com.bud.player.persistence.PlayerData;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DataCommand extends AbstractPlayerCommand {

    private final FlagArg deleteFlag;

    private final FlagArg deleteAllFlag;

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    public DataCommand() {
        super("data", "Manage player persisted data.");
        this.deleteFlag = this.withFlagArg("delete", "Delete persisted player data.");
        this.deleteAllFlag = this.withFlagArg("delete-all", "Delete all persisted player data.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.deleteAllFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Deleting all persisted player data by player " + playerRef.getUsername());
            // TODO: Implement actual deletion of all player data, currently just clears the
            // data for the executing player
        }
        if (this.deleteFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Deleting persisted player data for player " + playerRef.getUsername());
            ComponentType<EntityStore, PlayerData> componentType = BudPlugin.getInstance().getBudPlayerDataComponent();
            if (componentType != null) {
                store.putComponent(ref, componentType, new PlayerData());
                LoggerUtil.getLogger().info(() -> "[BUD] Cleared BudPlayerData for player " + playerRef.getUuid());
                this.chatInteraction.sendChatMessage(world, playerRef, "Cleared BudPlayerData.");
            }
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Show persisted player data for player " + playerRef.getUsername());
            ComponentType<EntityStore, PlayerData> componentType = BudPlugin.getInstance().getBudPlayerDataComponent();
            if (componentType != null) {
                PlayerData customData = store.ensureAndGetComponent(ref, componentType);
                String uuids = customData.getBuds().stream().map(UUID::toString).collect(Collectors.joining(","));
                LoggerUtil.getLogger()
                        .info(() -> "[BUD] Current BudPlayer: " + playerRef.getUuid() + " Data: " + uuids);
                this.chatInteraction.sendChatMessage(world, playerRef,
                        "Current BudPlayerData: " + uuids);
            }
        }
    }

}
