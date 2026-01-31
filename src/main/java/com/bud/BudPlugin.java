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
import com.bud.npcdata.persistence.BudPlayerData;

import java.util.concurrent.TimeUnit;

import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BudPlugin extends JavaPlugin {

    private static BudPlugin instance;
    private final Config<BudConfig> config;
    private ComponentType<EntityStore, BudPlayerData> budPlayerData;

    public BudPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("Bud", BudConfig.CODEC);
    }
    
    @Override
    protected void setup() {
        super.setup();
        BudConfig.setInstance(this.config.get());
        this.config.save();
        
        // Register persistent data
        this.budPlayerData = this.getEntityStoreRegistry().registerComponent(
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
            try {
                PlayerRef playerRef = event.getPlayerRef();
                System.err.println("[BUD] Player connected: " + playerRef.getUuid());
                System.err.println("[BUD] World: " + event.getWorld());
                IResult result = CleanUpHandler.removeOwnerBuds(playerRef);
                result.printResult();
            } catch (Exception e) {
                new ErrorResult("Fail during player connect event handling").printResult();
            }
        });
        

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            try {
                PlayerRef playerRef = event.getPlayerRef();
                System.err.println("[BUD] Player disconnected: " + playerRef.getUuid());
                IResult result = CleanUpHandler.removeOwnerBuds(playerRef);
                result.printResult();
            } catch (Exception e) {
                new ErrorResult("Fail during player disconnect event handling").printResult();
            }
        });

        if (BudConfig.get().isEnableLLM()) {
            // Schedule Random Chat Task (every 3 minutes)
            HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
                IResult result = NPCManager.getInstance().getStateTracker().triggerRandomChats();
                result.printResult();
            }, 180L, 180L, TimeUnit.SECONDS);
        }
    }

    public static BudPlugin instance() {
        return instance;
    }

    public ComponentType<EntityStore, BudPlayerData> getBudPlayerDataComponent() {
        return this.budPlayerData;
    }
}
