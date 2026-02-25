package com.bud.app;

import com.bud.app.commands.CreationCommand;
import com.bud.app.commands.DebugCommand;
import com.bud.app.commands.DeletionCommand;
import com.bud.app.commands.PromptCommand;
import com.bud.app.commands.ResetCommand;
import com.bud.app.commands.StateCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class BudCommandCollection extends AbstractCommandCollection {

    public BudCommandCollection() {
        super("bud", "Commands for managing Buds");
        this.addSubCommand(new CreationCommand());
        this.addSubCommand(new ResetCommand());
        this.addSubCommand(new DeletionCommand());
        this.addSubCommand(new StateCommand());
        this.addSubCommand(new PromptCommand());
        this.addSubCommand(new DebugCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

}
