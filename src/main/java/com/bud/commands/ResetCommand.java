package com.bud.commands;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.cleanup.CleanUpHandler;
import com.bud.events.BudCreationEvent;
import com.bud.events.ChatEvent;
import com.bud.profile.BudType;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ResetCommand extends AbstractPlayerCommand {

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
        this.cleanupBuds(playerRef, store, Set.of(BudType.VERI, BudType.KEYLETH, BudType.GRONKH));
        this.dispatchCreation(ref, Set.of(BudType.VERI, BudType.KEYLETH, BudType.GRONKH));
        ChatEvent.dispatch(playerRef, "Reset Buds for " + playerRef.getUsername() + ".");
    }

    private void cleanupBuds(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, Set<BudType> buds) {
        if (buds.isEmpty()) {
            return;
        }
        CleanUpHandler.cleanupBuds(playerRef, store, buds);
    }

    private void dispatchCreation(@Nonnull Ref<EntityStore> ref, Set<BudType> buds) {
        if (buds.isEmpty()) {
            return;
        }
        BudCreationEvent.dispatch(ref, buds);
    }

}
