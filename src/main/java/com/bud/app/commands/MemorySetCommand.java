package com.bud.app.commands;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.conversation.ConversationMemoryService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class MemorySetCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<String> budArg;

    @Nonnull
    private final RequiredArg<String> textArg;

    private final FlagArg legendaryFlag;

    public MemorySetCommand() {
        super("set", "Add a manual conversation memory.");
        this.budArg = Objects.requireNonNull(
                this.withRequiredArg("bud", "Bud name (veri, gronkh, keyleth).",
                        Objects.requireNonNull(ArgTypes.STRING)));
        this.textArg = Objects.requireNonNull(
                this.withRequiredArg("text", "Memory text.", Objects.requireNonNull(ArgTypes.GREEDY_STRING)));
        this.legendaryFlag = this.withFlagArg("legendary", "Add as a legendary memory instead.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String rawBudName = Objects.requireNonNull(context.get(this.budArg));
        String text = Objects.requireNonNull(context.get(this.textArg));

        String budDisplayName;
        try {
            budDisplayName = MemoryCommand.resolveBudDisplayName(rawBudName);
        } catch (IllegalArgumentException exception) {
            ChatEvent.dispatch(playerRef, "Unknown bud: " + rawBudName + ". Valid: veri, gronkh, keyleth.");
            return;
        }

        if (this.legendaryFlag.get(context)) {
            boolean stored = ConversationMemoryService.getInstance()
                    .addManualLegendaryMemory(playerRef.getUsername(), playerRef, budDisplayName, text);
            ChatEvent.dispatch(playerRef, stored
                    ? "Added legendary memory for " + budDisplayName + "."
                    : "Legendary memory slots for " + budDisplayName + " are full and no replacement was chosen.");
            return;
        }

        ConversationMemoryService.getInstance().addManualMemory(playerRef.getUsername(), playerRef, budDisplayName,
                text);
        ChatEvent.dispatch(playerRef, "Added memory for " + budDisplayName + ".");
    }

}
