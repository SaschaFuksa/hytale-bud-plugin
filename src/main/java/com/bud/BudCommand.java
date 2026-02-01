package com.bud;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.bud.interaction.BudChatInteraction;
import com.bud.npc.BudCreation;
import com.bud.npc.NPCManager;
import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudKweebecData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.bud.npcdata.persistence.BudPlayerData;
import com.bud.result.DataListResult;
import com.bud.result.IDataListResult;
import com.bud.result.IResult;
import com.bud.system.CleanUpHandler;
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
                case "data" -> {
                    BudPlayerData customData = store.ensureAndGetComponent(ref,
                            BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuids = customData.getBuds().stream().map(UUID::toString).collect(Collectors.joining(","));
                    System.out.println("[BUD] Current BudPlayerData for player " + playerRef.getUuid() + ": " + uuids);
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "Current BudPlayerData: " + uuids);
                }
                case "data-clean" -> {
                    store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), new BudPlayerData());
                    System.out.println("[BUD] Cleared BudPlayerData for player " + playerRef.getUuid());
                    this.chatInteraction.sendChatMessage(world, playerRef,
                            "Cleared BudPlayerData.");
                }
                default -> System.out.println("Unknown mode: " + inputMode);
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