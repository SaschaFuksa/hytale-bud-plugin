package com.bud.app.commands;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private final RequiredArg<Integer> idArg;

    private final FlagArg legendaryFlag;

    public MemoryDeleteCommand() {
        super("delete", "Delete a conversation memory by ID.");
        this.budArg = Objects.requireNonNull(
                this.withRequiredArg("bud", "Bud name (veri, gronkh, keyleth).",
                        Objects.requireNonNull(ArgTypes.STRING)));
        this.idArg = Objects.requireNonNull(
                this.withRequiredArg("id", "Memory ID shown by /bud memory. Stable, never reused.",
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
        long id = Objects.requireNonNull(context.get(this.idArg));

        String budDisplayName;
        try {
            budDisplayName = MemoryCommand.resolveBudDisplayName(rawBudName);
        } catch (IllegalArgumentException exception) {
            ChatEvent.dispatch(playerRef, "Unknown bud: " + rawBudName + ". Valid: veri, gronkh, keyleth.");
            return;
        }

        if (this.legendaryFlag.get(context)) {
            boolean removed = ConversationMemoryService.getInstance()
                    .removeLegendaryMemoryAt(playerRef.getUsername(), playerRef, budDisplayName, id);
            ChatEvent.dispatch(playerRef, removed
                    ? "Removed legendary memory #" + id + " for " + budDisplayName + "."
                    : "No legendary memory #" + id + " found for " + budDisplayName + ".");
            return;
        }

        List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                .getMemoriesForOwner(playerRef.getUsername());
        Optional<ConversationMemoryEntry> target = memories.stream().filter(memory -> memory.id() == id).findFirst();
        if (target.isEmpty()) {
            ChatEvent.dispatch(playerRef, "No memory #" + id + " found.");
            return;
        }
        if (!target.get().speakerName().equalsIgnoreCase(budDisplayName)) {
            ChatEvent.dispatch(playerRef,
                    "Memory #" + id + " belongs to " + target.get().speakerName() + ", not " + budDisplayName + ".");
            return;
        }

        ConversationMemoryEntry removed = ConversationMemoryService.getInstance()
                .removeMemoryAt(playerRef.getUsername(), playerRef, id);
        ChatEvent.dispatch(playerRef, removed != null
                ? "Removed memory #" + id + " for " + budDisplayName + "."
                : "No memory #" + id + " found.");
    }

}
