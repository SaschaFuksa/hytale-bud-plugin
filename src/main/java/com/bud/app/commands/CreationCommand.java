package com.bud.app.commands;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.registry.BudRegistry;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CreationCommand extends AbstractPlayerCommand {

    @Nonnull
    private final OptionalArg<String> budIdArg;

    public CreationCommand() {
        super("create", "Bud creation commands.");
        this.requireNoPermission();
        this.budIdArg = Objects.requireNonNull(this.withOptionalArg("bud",
                "Bud id to create. Omit to create the default roster (see roster.yml).",
                new BudIdArgumentType()));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String budId = context.get(this.budIdArg);
        if (budId != null) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Bud '" + budId + "' for player " + playerRef.getUsername());
            this.dispatchCreation(ref, Set.of(budId));
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating default Buds for player " + playerRef.getUsername());
            this.dispatchCreation(ref, new HashSet<>(BudRegistry.getInstance().getDefaultBudIds()));
        }
    }

    private void dispatchCreation(@Nonnull Ref<EntityStore> ref, Set<String> buds) {
        if (buds.isEmpty()) {
            return;
        }
        BudCreationEvent.dispatch(ref, buds);
    }

}
