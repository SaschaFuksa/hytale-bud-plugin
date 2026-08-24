package com.bud.app.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class ReloadCommand extends AbstractCommandCollection {

    public ReloadCommand() {
        super("reload", "Reload Bud content from disk.");
        this.addSubCommand(new ReloadBudsCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

}
