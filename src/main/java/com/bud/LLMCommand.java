package com.bud;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.bud.llm.BudLLM;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

/**
 * This is an example command that generates LLM jokes.
 */
public class LLMCommand extends CommandBase {
    private final BudLLM budLLM;

    public LLMCommand() {
        super("bud llm", "Generates an LLM joke from the Bud plugin.");
        this.setPermissionGroup(GameMode.Adventure);
        this.budLLM = new BudLLM();
    }
    
    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // Run async to avoid blocking the server
        Thread.ofVirtual().start(() -> {
            try {
                if (!budLLM.isEnabled()) {
                    ctx.sendMessage(Message.raw("LLM is disabled."));
                    return;
                }
                String content = budLLM.callLLM("Tell me a joke.");
                ctx.sendMessage(Message.raw(content));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                ctx.sendMessage(Message.raw("Error: " + e.getMessage()));
            }
        });
    }

}