package com.bud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.bud.systems.BudDamageFilterSystem;
import com.bud.systems.CleanUpHandler;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.HytaleServer;
import com.bud.npc.NPCManager;
import java.util.concurrent.TimeUnit;

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

        // Register Damage Filter System
        this.getEntityStoreRegistry().registerSystem(new BudDamageFilterSystem());

        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            System.err.println("[BUD] Player connected: " + playerRef.getUuid());
            System.err.println("[BUD] World: " + event.getWorld());
            if (event.getWorld() != null) {
                // Remove orphaned (untracked) buds in the world the player is joining.
                // This handles cleaning up buds from previous sessions/crashes since they aren't tracked anymore.
                CleanUpHandler.cleanOrphanedBuds(event.getWorld());
            }
        });
        

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            System.err.println("[BUD] Player disconnected: " + playerRef.getUuid());
            CleanUpHandler.removeOwnerBuds(playerRef);
        });

        // Schedule Random Chat Task (every 3 minutes)
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                NPCManager.getInstance().getStateTracker().triggerRandomChats();
            } catch (Exception e) {
                System.err.println("[BUD] Error in random chat task: " + e.getMessage());
            }
        }, 180L, 180L, TimeUnit.SECONDS);
    }
}
