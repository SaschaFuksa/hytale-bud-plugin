package com.bud;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.bud.app.BudCommandCollection;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.config.LLMConfig;
import com.bud.core.config.OrchestratorConfig;
import com.bud.core.config.ReactionConfig;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.block.BlockBreakFilterSystem;
import com.bud.feature.block.BlockPlaceFilterSystem;
import com.bud.feature.bud.BudRemovalSystem;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.bud.feature.bud.creation.BudCreationHandler;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.ChatHandler;
import com.bud.feature.combat.DamageFilterSystem;
import com.bud.feature.crafting.CraftRecipeFilterSystem;
import com.bud.feature.crafting.UseBlockFilterSystem;
import com.bud.feature.discover.DiscoverZoneFilterSystem;
import com.bud.feature.item.InventoryChangeListener;
import com.bud.feature.item.ItemPickupFilterSystem;
import com.bud.feature.player.PlayerJoinSystem;
import com.bud.feature.sound.SoundEvent;
import com.bud.feature.sound.SoundHandler;
import com.bud.feature.state.StateChangeEvent;
import com.bud.feature.state.StateChangeHandler;
import com.bud.feature.state.StateChangeSystem;
import com.bud.feature.teleport.TeleportEvent;
import com.bud.feature.teleport.TeleportFilterSystem;
import com.bud.feature.teleport.TeleportHandler;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.entity.LivingEntityInventoryChangeEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;

public class BudPlugin extends JavaPlugin {

    private static BudPlugin instance;

    private final Config<LLMConfig> llmConfig;
    private final Config<ReactionConfig> reactionConfig;
    private final Config<OrchestratorConfig> orchestratorConfig;
    private final Config<DebugConfig> debugConfig;

    @SuppressWarnings("null")
    public BudPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        this.llmConfig = this.withConfig("LLM", LLMConfig.CODEC);
        this.reactionConfig = this.withConfig("Reaction", ReactionConfig.CODEC);
        this.orchestratorConfig = this.withConfig("Orchestrator", OrchestratorConfig.CODEC);
        this.debugConfig = this.withConfig("Debug", DebugConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();

        this.setupLogging();
        this.setupConfig();
        LLMPromptManager.getInstance().reloadMissingPrompts();

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
                        "PlayerBudComponent",
                        PlayerBudComponent.CODEC);
        PlayerBudComponent.setComponentType(playerBudComponentType);

        // Register commands
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
        DebugConfig.setInstance(this.debugConfig.get());
        this.debugConfig.save();
    }

    private void registerEvents() {
        this.getEntityStoreRegistry().registerSystem(new BudRemovalSystem());
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
        this.getEntityStoreRegistry().registerSystem(new StateChangeSystem());

        this.getEventRegistry().register(ChatEvent.class, new ChatHandler());
        this.getEventRegistry().register(SoundEvent.class, new SoundHandler());
        this.getEventRegistry().register(BudCreationEvent.class, new BudCreationHandler());
        this.getEventRegistry().register(StateChangeEvent.class, new StateChangeHandler());
        this.getEventRegistry().register(TeleportEvent.class, new TeleportHandler());
    }

    public static BudPlugin getInstance() {
        return instance;
    }
}
