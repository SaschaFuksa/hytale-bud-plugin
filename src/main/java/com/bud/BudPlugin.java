package com.bud;

import java.util.UUID;

import javax.annotation.Nonnull;

import com.bud.cleanup.CleanUpHandler;
import com.bud.cleanup.CleanupSystem;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.player.persistence.PlayerData;
import com.bud.reaction.block.BlockBreakFilterSystem;
import com.bud.reaction.block.BlockChatScheduler;
import com.bud.reaction.block.BlockPlaceFilterSystem;
import com.bud.reaction.combat.CombatChatScheduler;
import com.bud.reaction.combat.DamageFilterSystem;
import com.bud.reaction.discover.DiscoverChatScheduler;
import com.bud.reaction.discover.DiscoverZoneFilterSystem;
import com.bud.reaction.farming.CraftRecipeFilterSystem;
import com.bud.reaction.farming.UseBlockFilterSystem;
import com.bud.reaction.item.InventoryChangeListener;
import com.bud.reaction.item.ItemChatScheduler;
import com.bud.reaction.item.ItemPickupFilterSystem;
import com.bud.reaction.tracker.MoodTracker;
import com.bud.result.ErrorResult;
import com.bud.result.IResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
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

    private static boolean startedMoodTracker = false;

    public BudPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("Bud", BudConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();

        this.setupLogging();
        this.setupConfig();

        // Register persistent data
        this.budPlayerData = this.getEntityStoreRegistry().registerComponent(
                PlayerData.class,
                "BudPlayerData",
                PlayerData.CODEC);

        // Register commands
        this.getCommandRegistry().registerCommand(new BudCommand(this));
        this.registerEvents();
    }

    private void setupLogging() {
        // Force log levels to ALL for debugging
        java.util.logging.Logger logger = LoggerUtil.getLogger();
        logger.setLevel(java.util.logging.Level.ALL);
        logger.info(() -> "[BUD] Logger name is: " + logger.getName());
    }

    private void setupConfig() {
        BudConfig.setInstance(this.config.get());
        this.config.save();

        LLMPromptManager.getInstance().reloadMissingPrompts();
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
            this.getEntityStoreRegistry().registerSystem(new BlockPlaceFilterSystem());
        }
        if (this.config.get().isEnableItemReactions()) {
            // Register inventory change listener for auto-pickup detection (e.g. ore)
            this.getEventRegistry().registerGlobal(
                    LivingEntityInventoryChangeEvent.class,
                    new InventoryChangeListener());
            this.getEntityStoreRegistry().registerSystem(new ItemPickupFilterSystem());
        }
        if (this.config.get().isEnableDiscoverReactions()) {
            this.getEntityStoreRegistry().registerSystem(new DiscoverZoneFilterSystem());
        }

        // Debug: Event listeners to discover farming/crafting interactions
        this.getEntityStoreRegistry().registerSystem(new UseBlockFilterSystem());
        this.getEntityStoreRegistry().registerSystem(new CraftRecipeFilterSystem());

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
             * On player connect, we need to register mood tracking and clean up any Bud
             * NPCs owned by the player
             * This clean up is triggered, if player has internet connection issues and the
             * NPCs were
             * not despawned by the disconnect event
             */
            if (!startedMoodTracker) {
                try {
                    LoggerUtil.getLogger().info(() -> "[BUD] Starting MoodTracker.");
                    MoodTracker.getInstance().startPolling();
                    startedMoodTracker = true;
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Failed to start MoodTracker: " + e.getMessage());
                }
            }
            try {
                PlayerRef playerRef = event.getPlayerRef();
                World world = event.getWorld();
                if (world == null) {
                    LoggerUtil.getLogger().warning(() -> "[BUD] World is null on player connect for player: "
                            + playerRef.getUuid());
                    return;
                }
                if (playerRef == null) {
                    LoggerUtil.getLogger().warning(() -> "[BUD] PlayerRef is null on player connect event");
                    return;
                }

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
                BlockChatScheduler.getInstance().clearPlayer(playerRef.getUuid());
                ItemChatScheduler.getInstance().clearPlayer(playerRef.getUuid());
                DiscoverChatScheduler.getInstance().clearPlayer(playerRef.getUuid());
                UUID worldUUID = playerRef.getWorldUuid();
                if (worldUUID != null) {
                    World world = Universe.get().getWorld(worldUUID);
                    if (world == null) {
                        LoggerUtil.getLogger().warning(() -> "[BUD] World not found for UUID: " + worldUUID);
                        return;
                    }
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

    public static BudPlugin getInstance() {
        return instance;
    }

    public ComponentType<EntityStore, PlayerData> getBudPlayerDataComponent() {
        return this.budPlayerData;
    }
}
