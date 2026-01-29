package com.bud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.bud.budworld.CleanUpHandler;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

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
            PlayerRef playerRef = event.getPlayerRef();
            System.err.println("[BUD] Player connected: " + playerRef.getUuid());
            System.err.println("[BUD] World: " + event.getWorld());
            if (event.getWorld() != null) {
                CleanUpHandler.removeOwnerBuds(playerRef);
            }
        });
        

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            System.err.println("[BUD] Player disconnected: " + playerRef.getUuid());
            CleanUpHandler.removeOwnerBuds(playerRef);
        });
    }

}
