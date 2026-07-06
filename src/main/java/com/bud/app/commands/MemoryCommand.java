package com.bud.app.commands;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.types.BudType;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.conversation.ConversationMemoryEntry;
import com.bud.feature.chat.conversation.ConversationMemoryService;
import com.bud.feature.profiles.BudProfileMapper;
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
        this.veriFlag = this.withFlagArg("veri", "Limit legendary memories to Veri.");
        this.keylethFlag = this.withFlagArg("keyleth", "Limit legendary memories to Keyleth.");
        this.gronkhFlag = this.withFlagArg("gronkh", "Limit legendary memories to Gronkh.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.legendaryFlag.get(context)) {
            this.sendLegendaryMemories(playerRef, this.resolveBudTypes(context));
        } else {
            this.sendNormalMemories(playerRef);
        }
    }

    @Nonnull
    private Set<BudType> resolveBudTypes(@Nonnull CommandContext context) {
        if (this.veriFlag.get(context)) {
            return Objects.requireNonNull(Set.of(BudType.VERI));
        }
        if (this.keylethFlag.get(context)) {
            return Objects.requireNonNull(Set.of(BudType.KEYLETH));
        }
        if (this.gronkhFlag.get(context)) {
            return Objects.requireNonNull(Set.of(BudType.GRONKH));
        }
        return Objects.requireNonNull(Set.of(BudType.values()));
    }

    private void sendNormalMemories(@Nonnull PlayerRef playerRef) {
        List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                .getMemoriesForOwner(playerRef.getUsername());
        if (memories.isEmpty()) {
            ChatEvent.dispatch(playerRef, "Memory: no memories stored yet.");
            return;
        }

        int index = 1;
        for (ConversationMemoryEntry memory : memories) {
            ChatEvent.dispatch(playerRef, "#" + index
                    + " [priority " + String.format("%.1f", memory.effectiveScore()) + "] "
                    + memory.speakerName() + ": " + memory.summary());
            index++;
        }
    }

    private void sendLegendaryMemories(@Nonnull PlayerRef playerRef, @Nonnull Set<BudType> budTypes) {
        boolean any = false;
        for (BudType budType : budTypes) {
            String budName = BudProfileMapper.getInstance().getProfileForBudType(budType).getNPCDisplayName();
            List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                    .getLegendaryMemoriesForBud(playerRef.getUsername(), budName);
            for (ConversationMemoryEntry memory : memories) {
                any = true;
                ChatEvent.dispatch(playerRef, "Legendary [" + budName + "]: " + memory.summary());
            }
        }
        if (!any) {
            ChatEvent.dispatch(playerRef, "Legendary Memory: none stored yet.");
        }
    }

}
