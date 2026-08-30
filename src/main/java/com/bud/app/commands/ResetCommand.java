package com.bud.app.commands;

import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.util.CleanupUtil;
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
        this.requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Resetting Bud system for player: " + playerRef.getUsername());
        this.cleanupBuds(playerRef, store, this.resolveCurrentBudIds(store, ref));
        this.dispatchCreation(ref, Set.copyOf(BudRegistry.getInstance().getDefaultBudIds()));
        ChatEvent.dispatch(playerRef, "Reset Buds for " + playerRef.getUsername() + ".");
    }

    @Nonnull
    private Set<String> resolveCurrentBudIds(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent.getComponentType());
        return playerBudComponent != null ? playerBudComponent.getBudIds() : Objects.requireNonNull(Set.of());
    }

    private void cleanupBuds(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, Set<String> buds) {
        if (buds.isEmpty()) {
            return;
        }
        CleanupUtil.cleanupBuds(playerRef, store, buds);
    }

    private void dispatchCreation(@Nonnull Ref<EntityStore> ref, Set<String> buds) {
        if (buds.isEmpty()) {
            return;
        }
        BudCreationEvent.dispatch(ref, buds);
    }

}
