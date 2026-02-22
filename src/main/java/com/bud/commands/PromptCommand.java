package com.bud.commands;

import javax.annotation.Nonnull;

import com.bud.interaction.ChatInteraction;
import com.bud.llm.messages.prompt.LLMPromptManager;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PromptCommand extends AbstractPlayerCommand {

    private final FlagArg reloadFlag;

    private final FlagArg resetFlag;

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    public PromptCommand() {
        super("prompt", "Manage Bud prompts.");
        this.reloadFlag = this.withFlagArg("reload",
                "Reload all Bud prompts for all players. Add missing, wont overwrite existing.");
        this.resetFlag = this.withFlagArg("reset", "Reset Bud prompts to default.");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (this.reloadFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Reloading Bud prompts by player: " + playerRef.getUsername());
            LLMPromptManager.getInstance().reloadMissingPrompts();
            this.chatInteraction.sendChatMessage(world, playerRef, "Reloaded prompts.");
        } else if (this.resetFlag.get(context)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Resetting Bud prompts by player: " + playerRef.getUsername());
            LLMPromptManager.getInstance().resetPrompts();
            this.chatInteraction.sendChatMessage(world, playerRef, "Reset prompts to default.");
        } else {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Reloading Bud prompts by player: " + playerRef.getUsername());
            LLMPromptManager.getInstance().reloadMissingPrompts();
            this.chatInteraction.sendChatMessage(world, playerRef, "Reloaded prompts.");
        }

    }
}
