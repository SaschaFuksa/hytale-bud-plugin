package com.bud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.bud.budworld.CleanUpHandler;
import com.bud.npc.NPCManager;
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
        this.config.save();
        
        // Register commands
        BudConfig budConfig = this.config.get();
        this.getCommandRegistry().registerCommand(new BudCommand(budConfig));
        this.getCommandRegistry().registerCommand(new LLMCommand(budConfig));

        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            if (event.getWorld() != null) {
                NPCManager manager = NPCManager.getInstance(budConfig);
                CleanUpHandler.handlePlayerConnect(event.getWorld(), manager.getTrackedBudRefs(), manager.getTrackedBudTypes());
            }
        });

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            CleanUpHandler.handlePlayerDisconnect(event.getPlayerRef(), NPCManager.getInstance(budConfig));
        });
    }

}
