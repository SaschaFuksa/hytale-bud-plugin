package com.bud.app.commands;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.profiles.BudType;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CreationCommand extends AbstractPlayerCommand {

    private final FlagArg allFlag;

    private final FlagArg veriFlag;

    private final FlagArg keylethFlag;

    private final FlagArg gronkhFlag;

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
            this.dispatchCreation(ref, Set.of(BudType.VERI, BudType.KEYLETH, BudType.GRONKH));
        } else if (this.veriFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Veri Bud for player " + playerRef.getUsername());
            this.dispatchCreation(ref, Set.of(BudType.VERI));
        } else if (this.keylethFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Keyleth Bud for player " + playerRef.getUsername());
            this.dispatchCreation(ref, Set.of(BudType.KEYLETH));
        } else if (this.gronkhFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Gronkh Bud for player " + playerRef.getUsername());
            this.dispatchCreation(ref, Set.of(BudType.GRONKH));
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating all Buds for player " + playerRef.getUsername());
            this.dispatchCreation(ref, Set.of(BudType.VERI, BudType.KEYLETH, BudType.GRONKH));
        }
    }

    private void dispatchCreation(@Nonnull Ref<EntityStore> ref, Set<BudType> buds) {
        if (buds.isEmpty()) {
            return;
        }
        BudCreationEvent.dispatch(ref, buds);
    }

}
