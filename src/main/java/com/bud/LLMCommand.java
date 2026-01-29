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
    private final BudConfig budConfig;
    private final BudLLM budLLM;

    public LLMCommand(BudConfig config) {
        super("bud llm", "Generates an LLM joke from the Bud plugin.");
        this.setPermissionGroup(GameMode.Adventure);
        this.budConfig = config;
        this.budLLM = new BudLLM(budConfig);
        System.out.println("[LLM] Config loaded - URL: " + budConfig.getUrl());
        System.out.println("[LLM] Model: " + budConfig.getModel());
    }
    
    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        // Run async to avoid blocking the server
        Thread.ofVirtual().start(() -> {
            try {
                if (!budConfig.isEnableLLM()) {
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