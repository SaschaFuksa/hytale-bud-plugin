package com.bud;

import javax.annotation.Nonnull;

import com.bud.interaction.ChatInteraction;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * This command spawns a Buddy that follows the player and can interact via LLM.
 * Uses a manual follow system that moves the NPC towards the player every tick.
 * Press F on the Bud to trigger LLM chat messages.
 */
public class BudCommand extends AbstractPlayerCommand {

    private final ChatInteraction chatInteraction;

    public BudCommand(BudPlugin budPlugin) {
        super("bud", "spawn bud.");
        this.addUsageVariant(new BudSetVariant());
        this.chatInteraction = ChatInteraction.getInstance();
    }

    /**
     * Disable automatic permission generation so the command is available to all
     * players.
     */
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
    }

    private static class BudSetVariant extends AbstractPlayerCommand {

        private final RequiredArg<String> modeArg;

        private final ChatInteraction chatInteraction;

        public BudSetVariant() {
            super("Manage Bud NPCs");
            this.modeArg = this.withRequiredArg("mode", "clean or clean-all", ArgTypes.STRING);
            this.chatInteraction = ChatInteraction.getInstance();
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext commandContext,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world) {

            String inputMode = this.modeArg.get(commandContext).toLowerCase();

            switch (inputMode) {
                default -> {
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "Unknown mode: " + inputMode + ". Valid modes: /bud: Spawn/teleport all buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud [Veri|Gronkh|Keyleth]: Spawn/teleport specific buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud [atk|attack|fol|follow|chill|stay]: Change current bud behavior");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud clean, /bud clear: Cleanup your buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud clean-all, /bud clear-all: Cleanup all buds in current world.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud reset: Cleanup and recreate all buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud data: Show your persisted data.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud clean-data, /bud clear-data: Clean your persisted data.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud reload-prompt: Reload prompts from disk, missing prompts will be re-copied from defaults.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud reset-prompt: Reset prompts, all prompts will be re-copied from defaults.");
                }
            }
        }

    }
}