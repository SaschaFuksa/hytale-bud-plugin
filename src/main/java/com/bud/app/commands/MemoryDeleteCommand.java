package com.bud.app.commands;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.conversation.ConversationMemoryEntry;
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

public class MemoryDeleteCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<String> budArg;

    @Nonnull
    private final RequiredArg<Integer> indexArg;

    private final FlagArg legendaryFlag;

    public MemoryDeleteCommand() {
        super("delete", "Delete a conversation memory by index.");
        this.budArg = Objects.requireNonNull(
                this.withRequiredArg("bud", "Bud name (veri, gronkh, keyleth).",
                        Objects.requireNonNull(ArgTypes.STRING)));
        this.indexArg = Objects.requireNonNull(
                this.withRequiredArg("index", "Memory index shown by /bud memory.",
                        Objects.requireNonNull(ArgTypes.INTEGER)));
        this.legendaryFlag = this.withFlagArg("legendary", "Delete a legendary memory instead.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String rawBudName = Objects.requireNonNull(context.get(this.budArg));
        int index = Objects.requireNonNull(context.get(this.indexArg));

        String budDisplayName;
        try {
            budDisplayName = MemoryCommand.resolveBudDisplayName(rawBudName);
        } catch (IllegalArgumentException exception) {
            ChatEvent.dispatch(playerRef, "Unknown bud: " + rawBudName + ". Valid: veri, gronkh, keyleth.");
            return;
        }

        if (this.legendaryFlag.get(context)) {
            boolean removed = ConversationMemoryService.getInstance()
                    .removeLegendaryMemoryAt(playerRef.getUsername(), playerRef, budDisplayName, index);
            ChatEvent.dispatch(playerRef, removed
                    ? "Removed legendary memory #" + index + " for " + budDisplayName + "."
                    : "No legendary memory #" + index + " found for " + budDisplayName + ".");
            return;
        }

        List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                .getMemoriesForOwner(playerRef.getUsername());
        if (index < 1 || index > memories.size()) {
            ChatEvent.dispatch(playerRef, "No memory #" + index + " found.");
            return;
        }
        ConversationMemoryEntry target = memories.get(index - 1);
        if (!target.speakerName().equalsIgnoreCase(budDisplayName)) {
            ChatEvent.dispatch(playerRef,
                    "Memory #" + index + " belongs to " + target.speakerName() + ", not " + budDisplayName + ".");
            return;
        }

        ConversationMemoryEntry removed = ConversationMemoryService.getInstance()
                .removeMemoryAt(playerRef.getUsername(), playerRef, index);
        ChatEvent.dispatch(playerRef, removed != null
                ? "Removed memory #" + index + " for " + budDisplayName + "."
                : "No memory #" + index + " found.");
    }

}
