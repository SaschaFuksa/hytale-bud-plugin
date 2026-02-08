package com.bud;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.bud.block.BlockBreakFilterSystem;
import com.bud.cleanup.CleanUpHandler;
import com.bud.cleanup.CleanupSystem;
import com.bud.combat.CombatChatScheduler;
import com.bud.combat.DamageFilterSystem;
import com.bud.interaction.InteractionManager;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.llm.message.world.LLMWorldManager;
import com.bud.npc.BudRegistry;
import com.bud.npc.persistence.PlayerData;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
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
    private ComponentType<EntityStore, PlayerData> budPlayerData;

    public BudPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("Bud", BudConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();

        // Force log levels to ALL for debugging
        java.util.logging.Logger logger = LoggerUtil.getLogger();
        logger.setLevel(java.util.logging.Level.ALL);
        logger.info(() -> "[BUD] Logger name is: " + logger.getName());

        BudConfig.setInstance(this.config.get());
        this.config.save();

        LLMPromptManager.getInstance().reload(false);

        // Register persistent data
        this.budPlayerData = this.getEntityStoreRegistry().registerComponent(
                PlayerData.class,
                "BudPlayerData",
                PlayerData.CODEC);

        // Register commands
        this.getCommandRegistry().registerCommand(new BudCommand(this));
        this.registerEvents();

    }

    private void registerEvents() {
        this.registerCleanupSystem();
        this.registerPlayerConnectEvent();
        this.registerPlayerDisconnectEvent();

        if (this.config.get().isEnableCombatReactions()) {
            // Register Damage Filter System
            this.getEntityStoreRegistry().registerSystem(new DamageFilterSystem());
        }
        if (this.config.get().isEnableBlockReactions()) {
            // Register Block Break Filter System
            this.getEntityStoreRegistry().registerSystem(new BlockBreakFilterSystem());
        }
        if (this.config.get().isEnableWorldReactions()) {
            // Register World Chat Scheduler
            this.registerWorldChatScheduler();
        }
    }

    private void registerCleanupSystem() {
        // Register Cleanup System
        /**
         * This Cleanup Stystem is triggered on server start
         * At server start, the unpersisted data are lost. Therefore, we need to clean
         * up any Bud NPCs
         */
        this.getEntityStoreRegistry().registerSystem(new CleanupSystem());

    }

    private void registerPlayerConnectEvent() {
        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            /**
             * On player connect, we need to clean up any Bud NPCs owned by the player
             * This is triggered, if player has internet connection issues and the NPCs were
             * not despawned by the disconnect event
             */
            try {
                @Nonnull
                PlayerRef playerRef = event.getPlayerRef();
                @Nonnull
                World world = event.getWorld();

                LoggerUtil.getLogger().fine(() -> "[BUD] Player connected: " + playerRef.getUuid());
                LoggerUtil.getLogger().fine(() -> "[BUD] World: " + world.getName());
                IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, world);
                result.printResult();
            } catch (Exception e) {
                new ErrorResult("Fail during player connect event handling").printResult();
            }
        });
    }

    private void registerPlayerDisconnectEvent() {
        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            /**
             * On player disconnect, we need to clean up any Bud NPCs owned by the player
             */
            try {
                @Nonnull
                PlayerRef playerRef = event.getPlayerRef();
                LoggerUtil.getLogger().fine(() -> "[BUD] Player disconnected: " + playerRef.getUuid());

                // Clear pending combat chat tasks for this player
                CombatChatScheduler.getInstance().clearPlayer(playerRef.getUuid());

                UUID worldUUID = playerRef.getWorldUuid();
                if (worldUUID != null) {
                    @Nonnull
                    World world = Universe.get().getWorld(worldUUID);
                    world.execute(() -> {
                        IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, world);
                        result.printResult();
                    });
                }
            } catch (Exception e) {
                new ErrorResult("Fail during player disconnect event handling").printResult();
            }
        });
    }

    private void registerWorldChatScheduler() {
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            IResult result = InteractionManager.getInstance()
                    .processInteraction(BudRegistry.getInstance().getAllOwners(), new LLMWorldManager());
            if (!result.isSuccess()) {
                result.printResult();
            }
        }, 90L, 90L, TimeUnit.SECONDS);
        LoggerUtil.getLogger().info(() -> "[BUD] Combat chat scheduler initialized (event-driven)");
    }

    public static BudPlugin getInstance() {
        return instance;
    }

    public ComponentType<EntityStore, PlayerData> getBudPlayerDataComponent() {
        return this.budPlayerData;
    }
}
