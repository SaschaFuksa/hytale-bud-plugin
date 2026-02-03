package com.bud;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.bud.interaction.BudChatInteraction;
import com.bud.npc.NPCManager;
import com.bud.npc.npccreation.BudCreation;
import com.bud.npc.npcdata.BudFeranData;
import com.bud.npc.npcdata.BudKweebecData;
import com.bud.npc.npcdata.BudTrorkData;
import com.bud.npc.npcdata.IBudNPCData;
import com.bud.npc.npcdata.persistence.BudPlayerData;
import com.bud.result.IDataListResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.bud.system.CleanUpHandler;
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

    private final BudChatInteraction chatInteraction;

    public BudCommand(BudPlugin budPlugin) {
        super("bud", "spawn bud.");
        this.addUsageVariant(new BudSetVariant());
        this.chatInteraction = new BudChatInteraction();
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {
        IDataListResult<NPCEntity> teleportResult = NPCManager.getInstance().teleportBuds(playerRef, store);
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

        private final BudChatInteraction chatInteraction;

        public BudSetVariant() {
            super("Manage Bud NPCs");
            this.modeArg = this.withRequiredArg("mode", "clean or clean-all", ArgTypes.STRING);
            this.chatInteraction = new BudChatInteraction();
        }

        @Override
        protected void execute(@NonNullDecl CommandContext commandContext,
                @NonNullDecl Store<EntityStore> store,
                @NonNullDecl Ref<EntityStore> ref,
                @NonNullDecl PlayerRef playerRef,
                @NonNullDecl World world) {

            String inputMode = this.modeArg.get(commandContext);

            switch (inputMode) {
                case BudFeranData.NPC_DISPLAY_NAME -> {
                    IResult result = executeBudAction(playerRef, store, new BudFeranData());
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case BudTrorkData.NPC_DISPLAY_NAME -> {
                    IResult result = executeBudAction(playerRef, store, new BudTrorkData());
                    result.printResult();
                    this.chatInteraction.sendChatMessage(world, playerRef, result.getMessage());
                }
                case BudKweebecData.NPC_DISPLAY_NAME -> {
                    IResult result = executeBudAction(playerRef, store, new BudKweebecData());
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
                    IDataListResult<NPCEntity> teleportResult = NPCManager.getInstance().teleportBuds(playerRef, store);
                    if (teleportResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef, teleportResult.getMessage());
                    }
                    IDataListResult<NPCEntity> creationResult = BudCreation.createBud(store, playerRef);
                    if (creationResult.isSuccess()) {
                        this.chatInteraction.sendChatMessage(world, playerRef, creationResult.getMessage());
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
                    BudPlayerData customData = store.ensureAndGetComponent(ref,
                            BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuids = customData.getBuds().stream().map(UUID::toString).collect(Collectors.joining(","));
                    LoggerUtil.getLogger()
                            .info(() -> "[BUD] Current BudPlayer: " + playerRef.getUuid() + " Data: " + uuids);
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "Current BudPlayerData: " + uuids);
                }
                case "data-clean" -> {
                    store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), new BudPlayerData());
                    LoggerUtil.getLogger().info(() -> "[BUD] Cleared BudPlayerData for player " + playerRef.getUuid());
                    this.chatInteraction.sendChatMessage(world, playerRef, "Cleared BudPlayerData.");
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
            Set<NPCEntity> buds = NPCManager.getInstance().getOwnedBuds(playerRef.getUuid(), store);
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
            IBudNPCData missingBud) {
        if (NPCManager.getInstance().canBeAdded(playerRef.getUuid(), store,
                missingBud)) {
            // Create new Bud
            return BudCreation.createBud(store, playerRef, Set.of(missingBud));
        } else {
            // Teleport existing Buds
            return NPCManager.getInstance().teleportBud(playerRef, store, missingBud);
        }
    }

}