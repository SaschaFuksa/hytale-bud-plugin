package com.bud;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bud.cleanup.CleanUpHandler;
import com.bud.interaction.ChatInteraction;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudManager;
import com.bud.npc.buds.GronkhData;
import com.bud.npc.buds.IBudData;
import com.bud.npc.buds.KeylethData;
import com.bud.npc.buds.VeriData;
import com.bud.npc.creation.BudCreation;
import com.bud.npc.persistence.PlayerData;
import com.bud.result.IDataListResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/**
 * This command spawns a Buddy that follows the player and can interact via LLM.
 * Uses a manual follow system that moves the NPC towards the player every tick.
 * Press F on the Bud to trigger LLM chat messages.
 */
public class BudCommand extends AbstractPlayerCommand {

    private final ChatInteraction chatInteraction;

    public BudCommand(BudPlugin budPlugin) {
        super("bud", "spawn bud.");
        this.addUsageVariant(new BudSetVariant());
        this.chatInteraction = ChatInteraction.getInstance();
    }

    /**
     * Disable automatic permission generation so the command is available to all
     * players.
     */
    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world) {
        IDataListResult<NPCEntity> teleportResult = BudManager.getInstance().teleportBuds(playerRef, store);
        if (teleportResult.isSuccess()) {
            this.chatInteraction.sendChatMessage(world, playerRef, teleportResult.getMessage());
        }
        IDataListResult<NPCEntity> creationResult = BudCreation.createBud(store, playerRef);
        if (creationResult.isSuccess()) {
            this.chatInteraction.sendChatMessage(world, playerRef, creationResult.getMessage());
        }
    }

    private static class BudSetVariant extends AbstractPlayerCommand {

        private final RequiredArg<String> modeArg;

        private final ChatInteraction chatInteraction;

        public BudSetVariant() {
            super("Manage Bud NPCs");
            this.modeArg = this.withRequiredArg("mode", "clean or clean-all", ArgTypes.STRING);
            this.chatInteraction = ChatInteraction.getInstance();
        }

        @Override
        protected boolean canGeneratePermission() {
            return false;
        }

        @Override
        protected void execute(@Nonnull CommandContext commandContext,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world) {

            String inputMode = this.modeArg.get(commandContext);

            switch (inputMode) {
                case VeriData.NPC_DISPLAY_NAME -> {
                    IResult result = executeBudAction(playerRef, store, new VeriData());
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case GronkhData.NPC_DISPLAY_NAME -> {
                    IResult result = executeBudAction(playerRef, store, new GronkhData());
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case KeylethData.NPC_DISPLAY_NAME -> {
                    IResult result = executeBudAction(playerRef, store, new KeylethData());
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case "attack", "atk" -> {
                    IResult stateResult = changeState(playerRef, store, "PetDefensive");
                    if (stateResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef, stateResult.getMessage());
                    }
                }
                case "follow", "fol" -> {
                    IResult stateResult = changeState(playerRef, store, "PetPassive");
                    if (stateResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef, stateResult.getMessage());
                    }
                }
                case "chill", "stay" -> {
                    IResult stateResult = changeState(playerRef, store, "PetSitting");
                    if (stateResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef, stateResult.getMessage());
                    }
                }
                case "reset" -> {
                    CleanUpHandler.cleanupOwnerBuds(playerRef, world).printResult();
                    IDataListResult<NPCEntity> teleportResult = BudManager.getInstance().teleportBuds(playerRef, store);
                    if (teleportResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef, teleportResult.getMessage());
                    }
                    IDataListResult<NPCEntity> creationResult = BudCreation.createBud(store, playerRef);
                    if (creationResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef,
                                "[BUD] " + creationResult.getMessage());
                    }
                }
                case "clean" -> {
                    IResult result = CleanUpHandler.cleanupOwnerBuds(playerRef, world);
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case "clean-all" -> {
                    IResult result = CleanUpHandler.cleanupAllBuds(world);
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case "data" -> {
                    PlayerData customData = store.ensureAndGetComponent(ref,
                            BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuids = customData.getBuds().stream().map(UUID::toString).collect(Collectors.joining(","));
                    LoggerUtil.getLogger()
                            .info(() -> "[BUD] Current BudPlayer: " + playerRef.getUuid() + " Data: " + uuids);
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "Current BudPlayerData: " + uuids);
                }
                case "data-clean" -> {
                    store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), new PlayerData());
                    LoggerUtil.getLogger().info(() -> "[BUD] Cleared BudPlayerData for player " + playerRef.getUuid());
                    this.chatInteraction.sendChatMessage(world, playerRef, "Cleared BudPlayerData.");
                }
                case "prompt-reload" -> {
                    LLMPromptManager.getInstance().reload(true);
                    LoggerUtil.getLogger().info(() -> "[BUD] Reloaded prompts.");
                    this.chatInteraction.sendChatMessage(world, playerRef, "Reloaded prompts.");
                }
                default -> {
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "Unknown mode: " + inputMode + ". Valid modes: /bud: Spawn/teleport all buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud [Veri|Gronkh|Keyleth]: Spawn/teleport specific buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud [atk|attack|fol|follow|chill|stay]: Change current bud behavior");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud clean: Cleanup your buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud clean-all: Cleanup all buds in current world.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud reset: Cleanup and recreate all buds.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud data: Show your persisted data.");
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "/bud data-clean: Clean your persisted data.");
                }
            }
        }

        private IResult changeState(PlayerRef playerRef, Store<EntityStore> store, String petState) {
            Set<NPCEntity> buds = BudManager.getInstance().getOwnedBuds(playerRef.getUuid(), store);
            boolean successed = false;
            for (NPCEntity bud : buds) {
                IResult result = BudCreation.changeRoleState(bud, playerRef, petState);
                if (result.isSuccess()) {
                    successed = true;
                }
            }
            if (successed) {
                String state = petState.replace("Pet", "");
                return new SuccessResult("Changed bud state to " + state + ".");
            } else {
                return new SuccessResult("No role changed.");
            }
        }

    }

    public static IResult executeBudAction(PlayerRef playerRef, Store<EntityStore> store,
            IBudData missingBud) {
        if (BudManager.getInstance().canBeAdded(playerRef.getUuid(), store,
                missingBud)) {
            // Create new Bud
            return BudCreation.createBud(store, playerRef, Set.of(missingBud));
        } else {
            // Teleport existing Buds
            return BudManager.getInstance().teleportBud(playerRef, store, missingBud);
        }
    }

}