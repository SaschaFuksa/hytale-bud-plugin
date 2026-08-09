package com.bud.app.commands;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.registry.BudRegistry;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.conversation.ConversationMemoryEntry;
import com.bud.feature.chat.conversation.ConversationMemoryService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class MemoryCommand extends AbstractPlayerCommand {

    private final FlagArg legendaryFlag;

    private final FlagArg veriFlag;

    private final FlagArg keylethFlag;

    private final FlagArg gronkhFlag;

    public MemoryCommand() {
        super("memory", "Query stored conversation memories.");
        this.legendaryFlag = this.withFlagArg("legendary", "Show legendary memories instead of normal memories.");
        this.veriFlag = this.withFlagArg("veri", "Limit memories to Veri.");
        this.keylethFlag = this.withFlagArg("keyleth", "Limit memories to Keyleth.");
        this.gronkhFlag = this.withFlagArg("gronkh", "Limit memories to Gronkh.");
        this.addSubCommand(new MemorySetCommand());
        this.addSubCommand(new MemoryDeleteCommand());
    }

    @Nonnull
    static String resolveBudDisplayName(@Nonnull String rawBudName) {
        String budId = BudRegistry.normalize(rawBudName);
        return BudRegistry.getInstance().get(budId).getDisplayName();
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.legendaryFlag.get(context)) {
            this.sendLegendaryMemories(playerRef, this.resolveBudIds(context));
        } else {
            this.sendNormalMemories(playerRef, this.resolveSingleBudFilter(context));
        }
    }

    @Nonnull
    private Set<String> resolveBudIds(@Nonnull CommandContext context) {
        if (this.veriFlag.get(context)) {
            return Objects.requireNonNull(Set.of("veri"));
        }
        if (this.keylethFlag.get(context)) {
            return Objects.requireNonNull(Set.of("keyleth"));
        }
        if (this.gronkhFlag.get(context)) {
            return Objects.requireNonNull(Set.of("gronkh"));
        }
        return Objects.requireNonNull(Set.of("veri", "keyleth", "gronkh"));
    }

    @Nullable
    private String resolveSingleBudFilter(@Nonnull CommandContext context) {
        if (this.veriFlag.get(context)) {
            return "veri";
        }
        if (this.keylethFlag.get(context)) {
            return "keyleth";
        }
        if (this.gronkhFlag.get(context)) {
            return "gronkh";
        }
        return null;
    }

    private void sendNormalMemories(@Nonnull PlayerRef playerRef, @Nullable String budFilter) {
        List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                .getMemoriesForOwner(playerRef.getUsername());
        if (memories.isEmpty()) {
            ChatEvent.dispatch(playerRef, "Memory: no memories stored yet.");
            return;
        }

        String budDisplayName = budFilter != null
                ? BudRegistry.getInstance().get(budFilter).getDisplayName()
                : null;

        boolean any = false;
        for (ConversationMemoryEntry memory : memories) {
            if (budDisplayName == null || memory.speakerName().equalsIgnoreCase(budDisplayName)) {
                any = true;
                ChatEvent.dispatch(playerRef, "#" + memory.id()
                        + " [priority " + String.format("%.1f", memory.effectiveScore()) + "] "
                        + memory.speakerName() + ": " + memory.summary());
            }
        }
        if (!any) {
            ChatEvent.dispatch(playerRef, "Memory: no memories stored yet for " + budDisplayName + ".");
        }
    }

    private void sendLegendaryMemories(@Nonnull PlayerRef playerRef, @Nonnull Set<String> budIds) {
        boolean any = false;
        for (String budId : budIds) {
            String budName = BudRegistry.getInstance().get(budId).getDisplayName();
            List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                    .getLegendaryMemoriesForBud(playerRef.getUsername(), budName);
            for (ConversationMemoryEntry memory : memories) {
                any = true;
                ChatEvent.dispatch(playerRef, "Legendary [" + budName + "] #" + memory.id() + ": " + memory.summary());
            }
        }
        if (!any) {
            ChatEvent.dispatch(playerRef, "Legendary Memory: none stored yet.");
        }
    }

}
