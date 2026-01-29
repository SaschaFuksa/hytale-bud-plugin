package com.bud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.bud.budworld.CleanUpHandler;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;

public class BudPlugin extends JavaPlugin {
    private final Config<BudConfig> config;
    private final CleanUpHandler cleanUpHandler;

    public BudPlugin(JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("Bud", BudConfig.CODEC);
        this.cleanUpHandler = new CleanUpHandler();
    }

    @Override
    protected void setup() {
        super.setup();
        this.config.save();
        
        // Register commands
        BudConfig budConfig = this.config.get();
        BudCommand budCommand = new BudCommand(budConfig);
        this.getCommandRegistry().registerCommand(budCommand);
        this.getCommandRegistry().registerCommand(new LLMCommand(budConfig));

        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            if (event.getWorld() != null) {
                this.cleanUpHandler.handlePlayerConnect(event.getWorld(), budCommand.getTrackedBudRefs(), budCommand.getTrackedBudTypes());
            }
        });

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            this.cleanUpHandler.handlePlayerDisconnect(event.getPlayerRef(), budCommand);
        });
    }

}
