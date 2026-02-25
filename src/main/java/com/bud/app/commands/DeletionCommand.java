package com.bud.app.commands;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.profiles.BudType;
import com.bud.feature.util.CleanupUtil;
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
            CleanupUtil.cleanupAllBuds(world, store);
        } else if (this.veriFlag.get(context)) {
            this.cleanupBuds(playerRef, store, Set.of(BudType.VERI));
        } else if (this.keylethFlag.get(context)) {
            this.cleanupBuds(playerRef, store, Set.of(BudType.KEYLETH));
        } else if (this.gronkhFlag.get(context)) {
            this.cleanupBuds(playerRef, store, Set.of(BudType.GRONKH));
        } else {
            this.cleanupBuds(playerRef, store, Set.of(BudType.VERI, BudType.KEYLETH, BudType.GRONKH));
        }
    }

    private void cleanupBuds(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, Set<BudType> buds) {
        if (buds.isEmpty()) {
            return;
        }
        CleanupUtil.cleanupBuds(playerRef, store, buds);
    }

}
