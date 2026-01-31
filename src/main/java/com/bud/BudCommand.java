package com.bud;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import com.bud.npc.BudCreation;
import com.bud.npc.NPCManager;
import com.bud.npcdata.BudFeranData;
import com.bud.npcdata.BudKweebecData;
import com.bud.npcdata.BudTrorkData;
import com.bud.npcdata.IBudNPCData;
import com.bud.npcdata.persistence.BudPlayerData;
import com.bud.result.ErrorResult;
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

/**
 * This command spawns a Buddy that follows the player and can interact via LLM.
 * Uses a manual follow system that moves the NPC towards the player every tick.
 * Press F on the Bud to trigger LLM chat messages.
 */
public class BudCommand extends AbstractPlayerCommand {

    public BudCommand(BudPlugin budPlugin) {
        super("bud", "spawn bud.");
        this.addUsageVariant(new BudSetVariant());
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world) {

        BudCreation.createBud(store, playerRef).printResult();
    }

    private static class BudSetVariant extends AbstractPlayerCommand {

        private final RequiredArg<String> modeArg;

        public BudSetVariant() {
            super("Manage Bud NPCs");
            this.modeArg = this.withRequiredArg("mode", "clean or clean-all", ArgTypes.STRING);
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
                }
                case "clean-all" -> {
                    IResult result = CleanUpHandler.cleanupAllBuds(world);
                    result.printResult();
                }
                case BudFeranData.NPC_DISPLAY_NAME -> {
                    try {
                        Set<IBudNPCData> missingBuds = Set.of(new BudFeranData());
                        if (NPCManager.getInstance().canBeAdded(playerRef.getUuid(), store,
                                missingBuds.iterator().next())) {
                            BudCreation.createBud(store, playerRef, missingBuds).printResult();
                        }
                    } catch (Exception e) {
                        new ErrorResult("Bud of type " + BudFeranData.NPC_TYPE_ID + " cannot be added.").printResult();
                    }
                }
                case BudTrorkData.NPC_DISPLAY_NAME -> {
                    try {
                        Set<IBudNPCData> missingBuds = Set.of(new BudTrorkData());
                        if (NPCManager.getInstance().canBeAdded(playerRef.getUuid(), store,
                                missingBuds.iterator().next())) {
                            BudCreation.createBud(store, playerRef, missingBuds).printResult();
                        }
                    } catch (Exception e) {
                        new ErrorResult("Bud of type " + BudTrorkData.NPC_TYPE_ID + " cannot be added.").printResult();
                    }
                }
                case BudKweebecData.NPC_DISPLAY_NAME -> {
                    try {
                        Set<IBudNPCData> missingBuds = Set.of(new BudKweebecData());
                        if (NPCManager.getInstance().canBeAdded(playerRef.getUuid(), store,
                                missingBuds.iterator().next())) {
                            BudCreation.createBud(store, playerRef, missingBuds).printResult();
                        }
                    } catch (Exception e) {
                        new ErrorResult("Bud of type " + BudKweebecData.NPC_TYPE_ID + " cannot be added.")
                                .printResult();
                    }
                }
                case "data" -> {
                    BudPlayerData customData = store.ensureAndGetComponent(ref,
                            BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuids = customData.getBuds().stream().map(UUID::toString).collect(Collectors.joining(","));
                    System.out.println("[BUD] Current BudPlayerData for player " + playerRef.getUuid() + ": " + uuids);
                    customData.resetBuds();
                    customData = store.ensureAndGetComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuidsAfterClear = customData.getBuds().stream().map(UUID::toString)
                            .collect(Collectors.joining(","));
                    System.out.println(
                            "[BUD] Current BudPlayerData for player " + playerRef.getUuid() + ": " + uuidsAfterClear);
                    UUID newUUID = UUID.randomUUID();
                    customData.add(newUUID);
                    store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), customData);
                    customData = store.ensureAndGetComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuidsAfterAdd = customData.getBuds().stream().map(UUID::toString)
                            .collect(Collectors.joining(","));
                    System.out.println("[BUD] BudPlayerData Loaded for player with new data " + playerRef.getUuid()
                            + ": " + uuidsAfterAdd);
                    customData.remove(newUUID);
                    store.putComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent(), customData);
                    customData = store.ensureAndGetComponent(ref, BudPlugin.getInstance().getBudPlayerDataComponent());
                    String uuidsAfterRemove = customData.getBuds().stream().map(UUID::toString)
                            .collect(Collectors.joining(","));
                    System.out.println("[BUD] BudPlayerData Loaded for player with removed data " + playerRef.getUuid()
                            + ": " + uuidsAfterRemove);
                }
                default -> System.out.println("Unknown mode: " + inputMode);
            }
        }

    }

}