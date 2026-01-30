package com.bud;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.bud.system.BudDamageFilterSystem;
import com.bud.system.CleanUpHandler;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.HytaleServer;
import com.bud.npc.NPCManager;
import com.bud.npcdata.BudPlayerData;

import java.util.concurrent.TimeUnit;

import com.bud.result.IResult;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BudPlugin extends JavaPlugin {
    private final Config<BudConfig> config;
    public static ComponentType<EntityStore, BudPlayerData> BUD_PLAYER_DATA;

    public BudPlugin(JavaPluginInit init) {
        super(init);
        this.config = this.withConfig("Bud", BudConfig.CODEC);
    }
    
    @Override
    protected void setup() {
        super.setup();
        BudConfig.setInstance(this.config.get());
        this.config.save();
        
        // Register persistent data
        BUD_PLAYER_DATA = this.getEntityStoreRegistry().registerComponent(
            BudPlayerData.class,
            "BudPlayerData",
            BudPlayerData.CODEC
        );
        
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
                IResult result = CleanUpHandler.removeOwnerBuds(playerRef);
                result.printResult();
            }
        });
        

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            System.err.println("[BUD] Player disconnected: " + playerRef.getUuid());
            IResult result = CleanUpHandler.removeOwnerBuds(playerRef);
            result.printResult();
        });

        if (BudConfig.get().isEnableLLM()) {
            // Schedule Random Chat Task (every 3 minutes)
            HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
                IResult result = NPCManager.getInstance().getStateTracker().triggerRandomChats();
                result.printResult();
            }, 180L, 180L, TimeUnit.SECONDS);
        }
    }
}
