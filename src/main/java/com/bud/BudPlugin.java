package com.bud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.bud.budworld.CleanUpHandler;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;

public class BudPlugin extends JavaPlugin {
    private final Config<BudConfig> config;

    public BudPlugin(JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("Bud", BudConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        BudConfig.setInstance(this.config.get());
        this.config.save();
        
        // Register commands
        this.getCommandRegistry().registerCommand(new BudCommand(this));
        this.getCommandRegistry().registerCommand(new LLMCommand());

        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            if (event.getWorld() != null) {
                CleanUpHandler.removeOwnerBuds(event.getPlayerRef());
            }
        });
        

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            CleanUpHandler.removeOwnerBuds(event.getPlayerRef());
        });
    }

}
