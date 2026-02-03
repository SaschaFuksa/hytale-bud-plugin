package com.bud;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.bud.llm.BudLLMRandomChat;
import com.bud.llm.llmcombatmessage.CombatChatScheduler;
import com.bud.llm.llmworldmessage.LLMChatWorldContext;
import com.bud.npc.npcdata.persistence.BudPlayerData;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.bud.system.BudCleanupSystem;
import com.bud.system.BudDamageFilterSystem;
import com.bud.system.CleanUpHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

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
                BudPlayerData.CODEC);

        // Register commands
        this.getCommandRegistry().registerCommand(new BudCommand(this));

        // Register Cleanup System
        /**
         * This Cleanup Stystem is triggered on server start
         * At server start, the unpersisted data are lost. Therefore, we need to clean
         * up any Bud NPCs
         */
        this.getEntityStoreRegistry().registerSystem(new BudCleanupSystem());

        // TODO: For future implementation of YAML prompt config files
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.hashCode();

        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            /**
             * On player connect, we need to clean up any Bud NPCs owned by the player
             * This is triggered, if player has internet connection issues and the NPCs were
             * not despawned by the disconnect event
             */
            try {
                PlayerRef playerRef = event.getPlayerRef();
                LoggerUtil.getLogger().fine(() -> "[BUD] Player connected: " + playerRef.getUuid());
                LoggerUtil.getLogger().fine(() -> "[BUD] World: " + event.getWorld());
                IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, event.getWorld());
                result.printResult();
            } catch (Exception e) {
                new ErrorResult("Fail during player connect event handling").printResult();
            }
        });

        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            /**
             * On player disconnect, we need to clean up any Bud NPCs owned by the player
             */
            try {
                PlayerRef playerRef = event.getPlayerRef();
                LoggerUtil.getLogger().fine(() -> "[BUD] Player disconnected: " + playerRef.getUuid());

                // Clear pending combat chat tasks for this player
                CombatChatScheduler.getInstance().clearPlayer(playerRef.getUuid());

                UUID worldUUID = playerRef.getWorldUuid();
                if (worldUUID != null) {
                    World world = Universe.get().getWorld(worldUUID);
                    if (world != null) {
                        world.execute(() -> {
                            IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, world);
                            result.printResult();
                        });
                    }
                }
            } catch (Exception e) {
                new ErrorResult("Fail during player disconnect event handling").printResult();
            }
        });

        if (BudConfig.get().isEnableLLM()) {
            // Register Damage Filter System
            this.getEntityStoreRegistry().registerSystem(new BudDamageFilterSystem());
            // Schedule Random World Chat Task (every 2 minutes)
            // World chats are still polled since they are time-based, not event-driven
            HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
                IResult result = BudLLMRandomChat.getInstance().triggerRandomLLMChats(new LLMChatWorldContext());
                if (!result.isSuccess()) {
                    result.printResult();
                }
            }, 120L, 120L, TimeUnit.SECONDS);
            LoggerUtil.getLogger().info(() -> "[BUD] Combat chat scheduler initialized (event-driven)");
            registerLLMFeatures();
        }
    }

    private void registerLLMFeatures() {
    }

    public static BudPlugin getInstance() {
        return instance;
    }

    public ComponentType<EntityStore, BudPlayerData> getBudPlayerDataComponent() {
        return this.budPlayerData;
    }
}
