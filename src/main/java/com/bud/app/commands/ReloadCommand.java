package com.bud.app.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/** Command group for reloading content areas that don't already have their own top-level command. */
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
