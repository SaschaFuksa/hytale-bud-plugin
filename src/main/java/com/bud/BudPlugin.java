package com.bud;

import java.util.logging.Level;

import com.bud.app.BudCommandCollection;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ConversationConfig;
import com.bud.core.config.DebugConfig;
import com.bud.core.config.LLMConfig;
import com.bud.core.config.OrchestratorConfig;
import com.bud.core.config.ReactionConfig;
import com.bud.core.config.WorkConfig;
import com.bud.core.registry.BudRegistry;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.block.BlockBreakFilterSystem;
import com.bud.feature.block.BlockPlaceFilterSystem;
import com.bud.feature.bud.BudRemovalSystem;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.bud.feature.bud.creation.BudCreationHandler;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.ChatHandler;
import com.bud.feature.chat.player.PlayerChatReactionHandler;
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
import com.bud.feature.work.BuilderRestTargetReachedSensor;
import com.bud.feature.work.BuilderRestTargetSensor;
import com.bud.feature.work.BuilderWorkTargetSensor;
import com.bud.feature.work.WorkRecipeConfig;
import com.bud.feature.work.WorkstationBlockEntity;
import com.bud.feature.work.WorkstationFilterSystem;
import com.bud.feature.work.WorkstationFuelTickSystem;
import com.bud.feature.work.farming.BuilderActionFarmWork;
import com.bud.feature.work.lumbering.BuilderActionLumberingWork;
import com.bud.feature.work.mining.BuilderActionMiningWork;
import com.bud.feature.work.lumbering.TreeGrowthTickSystem;
import com.bud.feature.work.mining.OreGrowthBlock;
import com.bud.feature.work.mining.OreGrowthTickSystem;
import com.bud.feature.work.reaction.BuilderActionWorkTalk;
import com.bud.interaction.CardBudInteraction;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.NPCPlugin;

public class BudPlugin extends JavaPlugin {

    private static BudPlugin instance;

    private final Config<LLMConfig> llmConfig;
    private final Config<ReactionConfig> reactionConfig;
    private final Config<OrchestratorConfig> orchestratorConfig;
    private final Config<ConversationConfig> conversationConfig;
    private final Config<DebugConfig> debugConfig;
    private final Config<WorkConfig> workConfig;

    @SuppressWarnings("null")
    public BudPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
        this.llmConfig = this.withConfig("LLM", LLMConfig.CODEC);
        this.reactionConfig = this.withConfig("Reaction", ReactionConfig.CODEC);
        this.orchestratorConfig = this.withConfig("Orchestrator", OrchestratorConfig.CODEC);
        this.conversationConfig = this.withConfig("Conversation", ConversationConfig.CODEC);
        this.debugConfig = this.withConfig("Debug", DebugConfig.CODEC);
        this.workConfig = this.withConfig("Work", WorkConfig.CODEC);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCodecRegistry(Interaction.CODEC).register("CardBud", CardBudInteraction.class,
                CardBudInteraction.CODEC_CARD_BUD);
        this.setupConfig();
        this.setupLogging();
        LLMPromptManager.getInstance().reloadMissingPrompts();
        BudRegistry.getInstance().reloadMissing();
        WorkRecipeConfig.getInstance().reloadMissing();

        ComponentType<EntityStore, BudComponent> budComponentType = this.getEntityStoreRegistry().registerComponent(
                BudComponent.class,
                "BudComponent",
                BudComponent.CODEC);
        BudComponent.setComponentType(budComponentType);

        ComponentType<EntityStore, PlayerBudComponent> playerBudComponentType = this.getEntityStoreRegistry()
                .registerComponent(
                        PlayerBudComponent.class,
                        "PlayerBudComponent",
                        PlayerBudComponent.CODEC);
        PlayerBudComponent.setComponentType(playerBudComponentType);

        ComponentType<ChunkStore, WorkstationBlockEntity> workstationBlockEntityType = this.getChunkStoreRegistry()
                .registerComponent(
                        WorkstationBlockEntity.class,
                        "WorkstationBlockEntity",
                        WorkstationBlockEntity.CODEC);
        WorkstationBlockEntity.setComponentType(workstationBlockEntityType);
        this.getChunkStoreRegistry().registerSystem(new WorkstationFilterSystem());
        this.getChunkStoreRegistry().registerSystem(new WorkstationFuelTickSystem());

        ComponentType<ChunkStore, OreGrowthBlock> oreGrowthBlockType = this.getChunkStoreRegistry()
                .registerComponent(
                        OreGrowthBlock.class,
                        "OreGrowthBlock",
                        OreGrowthBlock.CODEC);
        OreGrowthBlock.setComponentType(oreGrowthBlockType);
        this.getChunkStoreRegistry().registerSystem(new OreGrowthTickSystem());
        this.getChunkStoreRegistry().registerSystem(new TreeGrowthTickSystem());

        NPCPlugin.get().registerCoreComponentType("FarmWork", BuilderActionFarmWork::new);
        NPCPlugin.get().registerCoreComponentType("LumberingWork", BuilderActionLumberingWork::new);
        NPCPlugin.get().registerCoreComponentType("MiningWork", BuilderActionMiningWork::new);
        NPCPlugin.get().registerCoreComponentType("WorkTarget", BuilderWorkTargetSensor::new);
        NPCPlugin.get().registerCoreComponentType("RestTarget", BuilderRestTargetSensor::new);
        NPCPlugin.get().registerCoreComponentType("RestTargetReached", BuilderRestTargetReachedSensor::new);
        NPCPlugin.get().registerCoreComponentType("WorkTalk", BuilderActionWorkTalk::new);

        this.getCommandRegistry().registerCommand(new BudCommandCollection());
        this.registerEvents();
    }

    private void setupLogging() {
        Level level;
        try {
            level = Level.parse(DebugConfig.getInstance().getLogLevel());
        } catch (IllegalArgumentException exception) {
            level = Level.INFO;
        }
        LoggerUtil.getLogger().setLevel(level);
        LoggerUtil.getLogger().info(() -> "[BUD] Logger name is: " + LoggerUtil.getLogger().getName());
    }

    private void setupConfig() {
        LLMConfig.setInstance(this.llmConfig.get());
        this.llmConfig.save();
        ReactionConfig.setInstance(this.reactionConfig.get());
        this.reactionConfig.save();
        OrchestratorConfig.setInstance(this.orchestratorConfig.get());
        this.orchestratorConfig.save();
        ConversationConfig.setInstance(this.conversationConfig.get());
        this.conversationConfig.save();
        DebugConfig.setInstance(this.debugConfig.get());
        this.debugConfig.save();
        WorkConfig.setInstance(this.workConfig.get());
        this.workConfig.save();
    }

    private void registerEvents() {
        this.getEntityStoreRegistry().registerSystem(new BudRemovalSystem());
        this.getEntityStoreRegistry().registerSystem(new PlayerJoinSystem());

        if (this.reactionConfig.get().isEnableCombatReactions()) {
            this.getEntityStoreRegistry().registerSystem(new DamageFilterSystem());
        }
        if (this.reactionConfig.get().isEnableBlockReactions()) {
            this.getEntityStoreRegistry().registerSystem(new BlockBreakFilterSystem());
            this.getEntityStoreRegistry().registerSystem(new BlockPlaceFilterSystem());
        }
        if (this.reactionConfig.get().isEnableItemReactions()) {
            this.getEntityStoreRegistry().registerSystem(new InventoryChangeListener());
            this.getEntityStoreRegistry().registerSystem(new ItemPickupFilterSystem());
        }
        if (this.reactionConfig.get().isEnableDiscoverReactions()) {
            this.getEntityStoreRegistry().registerSystem(new DiscoverZoneFilterSystem());
        }
        if (this.reactionConfig.get().isEnableCraftingReactions()) {
            this.getEntityStoreRegistry().registerSystem(new CraftRecipeFilterSystem());
            this.getEntityStoreRegistry().registerSystem(new UseBlockFilterSystem());
        }
        if (this.reactionConfig.get().isEnablePlayerChatReactions()) {
            this.getEventRegistry().registerGlobal(PlayerChatEvent.class, new PlayerChatReactionHandler());
        }

        this.getEntityStoreRegistry().registerSystem(new TeleportFilterSystem());

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
