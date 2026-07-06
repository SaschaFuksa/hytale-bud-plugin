package com.bud.app.commands;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.BudManager;
import com.bud.core.types.BudType;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.util.CleanupUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DeletionCommand extends AbstractPlayerCommand {

    private static final String ADMIN_GROUP = "hytale:Admin";

    private final FlagArg veriFlag;

    private final FlagArg keylethFlag;

    private final FlagArg gronkhFlag;

    private final FlagArg worldFlag;

    private final OptionalArg<String> playerNameArg;

    public DeletionCommand() {
        super("delete", "Delete Bud commands.");
        this.veriFlag = this.withFlagArg("veri", "Delete Veri Bud.");
        this.keylethFlag = this.withFlagArg("keyleth", "Delete Keyleth Bud.");
        this.gronkhFlag = this.withFlagArg("gronkh", "Delete Gronkh Bud.");
        this.worldFlag = this.withFlagArg("world", "Delete all Buds in all worlds. Requires admin permission.");
        this.playerNameArg = Objects.requireNonNull(this.withOptionalArg("playername",
                "Delete Buds of a specific player by username. Requires admin permission for other players.",
                Objects.requireNonNull(ArgTypes.STRING)));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.worldFlag.get(context)) {
            if (!this.requireAdmin(playerRef)) {
                return;
            }
            CleanupUtil.cleanupAllBuds(world, store);
            return;
        }

        PlayerRef targetPlayerRef = playerRef;
        String targetPlayerName = null;
        if (this.playerNameArg != null) {
            targetPlayerName = context.get(this.playerNameArg);
        }
        if (targetPlayerName != null && !targetPlayerName.isBlank()
                && !targetPlayerName.equalsIgnoreCase(playerRef.getUsername())) {
            if (!this.requireAdmin(playerRef)) {
                return;
            }
            targetPlayerRef = this.resolvePlayer(targetPlayerName);
            if (targetPlayerRef == null) {
                ChatEvent.dispatch(playerRef, "Player not found or not online: " + targetPlayerName);
                return;
            }
        }

        if (this.veriFlag.get(context)) {
            this.cleanupBuds(targetPlayerRef, store, Set.of(BudType.VERI));
        } else if (this.keylethFlag.get(context)) {
            this.cleanupBuds(targetPlayerRef, store, Set.of(BudType.KEYLETH));
        } else if (this.gronkhFlag.get(context)) {
            this.cleanupBuds(targetPlayerRef, store, Set.of(BudType.GRONKH));
        } else {
            this.cleanupBuds(targetPlayerRef, store, Set.of(BudType.VERI, BudType.KEYLETH, BudType.GRONKH));
        }
    }

    @Nullable
    private PlayerRef resolvePlayer(@Nonnull String username) {
        return BudManager.getInstance().getTrackedPlayers().stream()
                .filter(p -> p.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    private boolean requireAdmin(@Nonnull PlayerRef playerRef) {
        if (PermissionsModule.get().getGroupsForUser(playerRef.getUuid()).contains(ADMIN_GROUP)) {
            return true;
        }
        ChatEvent.dispatch(playerRef, "You don't have permission to do this.");
        return false;
    }

    private void cleanupBuds(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, Set<BudType> buds) {
        if (buds.isEmpty()) {
            return;
        }
        CleanupUtil.cleanupBuds(playerRef, store, buds);
    }

}
