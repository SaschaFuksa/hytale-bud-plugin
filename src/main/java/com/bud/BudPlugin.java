package com.bud;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.bud.cleanup.CleanupSystem;
import com.bud.commands.BudCommandCollection;
import com.bud.components.BudComponent;
import com.bud.components.PlayerBudComponent;
import com.bud.config.LLMConfig;
import com.bud.config.OrchestratorConfig;
import com.bud.config.ReactionConfig;
import com.bud.events.ChatEvent;
import com.bud.events.SoundEvent;
import com.bud.handler.ChatHandler;
import com.bud.handler.SoundHandler;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.orchestrator.MessageOrchestrator;
import com.bud.player.persistence.PlayerData;
import com.bud.reaction.block.BlockBreakFilterSystem;
import com.bud.reaction.block.BlockPlaceFilterSystem;
import com.bud.reaction.combat.DamageFilterSystem;
import com.bud.reaction.crafting.CraftRecipeFilterSystem;
import com.bud.reaction.crafting.UseBlockFilterSystem;
import com.bud.reaction.discover.DiscoverZoneFilterSystem;
import com.bud.reaction.item.InventoryChangeListener;
import com.bud.reaction.item.ItemPickupFilterSystem;
import com.bud.reaction.state.BudStateChangeSystem;
import com.bud.reaction.teleport.TeleportFilterSystem;
import com.bud.reaction.tracker.MoodTracker;
import com.bud.result.ErrorResult;
import com.bud.systems.BudRemovalSystem;
import com.bud.systems.PlayerJoinSystem;
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

    private final Config<LLMConfig> llmConfig;
    private final Config<ReactionConfig> reactionConfig;
    private final Config<OrchestratorConfig> orchestratorConfig;

    private ComponentType<EntityStore, PlayerData> budPlayerData;

    private static boolean startedMoodTracker = false;
    private static boolean startedOrchestrator = false;

    @SuppressWarnings("null")
    public BudPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        this.llmConfig = this.withConfig("LLM", LLMConfig.CODEC);
        this.reactionConfig = this.withConfig("Reaction", ReactionConfig.CODEC);
        this.orchestratorConfig = this.withConfig("Orchestrator", OrchestratorConfig.CODEC);
    }

    @Override
    @SuppressWarnings("null")
    protected void setup() {
        super.setup();

        this.setupLogging();
        this.setupConfig();
        LLMPromptManager.getInstance().reloadMissingPrompts();

        // Register persistent data
        this.budPlayerData = this.getEntityStoreRegistry().registerComponent(
                PlayerData.class,
                "BudPlayerData",
                PlayerData.CODEC);

        // Register BudComponent for state tracking
        ComponentType<EntityStore, BudComponent> budComponentType = this.getEntityStoreRegistry().registerComponent(
                BudComponent.class,
                "BudComponent",
                BudComponent.CODEC);
        BudComponent.setComponentType(budComponentType);

        // Register PlayerBudComponent for tracking player's Buds
        ComponentType<EntityStore, PlayerBudComponent> playerBudComponentType = this.getEntityStoreRegistry()
                .registerComponent(
                        PlayerBudComponent.class,
                        "PlayerBuddiesComponent",
                        PlayerBudComponent.CODEC);
        PlayerBudComponent.setComponentType(playerBudComponentType);

        // Register commands
        // TODO: REMOVE this.getCommandRegistry().registerCommand(new BudCommand(this));
        this.getCommandRegistry().registerCommand(new BudCommandCollection());
        this.registerEvents();
    }

    private void setupLogging() {
        // Force log levels to ALL for debugging
        Logger logger = LoggerUtil.getLogger();
        logger.setLevel(Level.ALL);
        logger.info(() -> "[BUD] Logger name is: " + logger.getName());
    }

    private void setupConfig() {
        LLMConfig.setInstance(this.llmConfig.get());
        this.llmConfig.save();
        ReactionConfig.setInstance(this.reactionConfig.get());
        this.reactionConfig.save();
        OrchestratorConfig.setInstance(this.orchestratorConfig.get());
        this.orchestratorConfig.save();
    }

    private void registerEvents() {
        this.registerCleanupSystem();
        this.registerPlayerConnectEvent();
        // this.registerPlayerDisconnectEvent();
        this.getEntityStoreRegistry().registerSystem(new PlayerJoinSystem());

        if (this.reactionConfig.get().isEnableCombatReactions()) {
            // Register Damage Filter System
            this.getEntityStoreRegistry().registerSystem(new DamageFilterSystem());
        }
        if (this.reactionConfig.get().isEnableBlockReactions()) {
            // Register Block Break Filter System
            this.getEntityStoreRegistry().registerSystem(new BlockBreakFilterSystem());
            this.getEntityStoreRegistry().registerSystem(new BlockPlaceFilterSystem());
        }
        if (this.reactionConfig.get().isEnableItemReactions()) {
            // Register inventory change listener for auto-pickup detection (e.g. ore)
            this.getEventRegistry().registerGlobal(
                    LivingEntityInventoryChangeEvent.class,
                    new InventoryChangeListener());
            this.getEntityStoreRegistry().registerSystem(new ItemPickupFilterSystem());
        }
        if (this.reactionConfig.get().isEnableDiscoverReactions()) {
            this.getEntityStoreRegistry().registerSystem(new DiscoverZoneFilterSystem());
        }
        if (this.reactionConfig.get().isEnableCraftingReactions()) {
            this.getEntityStoreRegistry().registerSystem(new CraftRecipeFilterSystem());
            this.getEntityStoreRegistry().registerSystem(new UseBlockFilterSystem());
        }

        // Register Teleport Filter System (always enabled for debugging)
        this.getEntityStoreRegistry().registerSystem(new TeleportFilterSystem());

        // Register Bud State Change System for detecting NPC state changes
        this.getEntityStoreRegistry().registerSystem(new BudStateChangeSystem());

        this.getEventRegistry().register(ChatEvent.class, new ChatHandler());
        this.getEventRegistry().register(SoundEvent.class, new SoundHandler());

    }

    private void registerCleanupSystem() {
        // Register Cleanup System
        /**
         * This Cleanup Stystem is triggered on server start
         * At server start, the unpersisted data are lost. Therefore, we need to clean
         * up any Bud NPCs
         */
        this.getEntityStoreRegistry().registerSystem(new CleanupSystem());
        this.getEntityStoreRegistry().registerSystem(new BudRemovalSystem());

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
            if (!startedOrchestrator) {
                try {
                    LoggerUtil.getLogger().info(() -> "[BUD] Starting MessageOrchestrator.");
                    MessageOrchestrator.getInstance().start();
                    startedOrchestrator = true;
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Failed to start MessageOrchestrator: " + e.getMessage());
                }
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

                // Clear pending orchestrator state for this player
                MessageOrchestrator.getInstance().clearPlayer(playerRef.getUuid());
                UUID worldUUID = playerRef.getWorldUuid();
                if (worldUUID != null) {
                    World world = Universe.get().getWorld(worldUUID);
                    if (world == null) {
                        LoggerUtil.getLogger().warning(() -> "[BUD] World not found for UUID: " + worldUUID);
                        return;
                    }
                    // world.execute(() -> {
                    // IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, world);
                    // result.printResult();
                    // });
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
