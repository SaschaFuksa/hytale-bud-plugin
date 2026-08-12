package com.bud.feature.bud.creation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.DebugConfig;
import com.bud.core.debug.BudDebugInfo;
import com.bud.core.registry.BudDefinition;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.BudState;
import com.bud.feature.bud.reaction.BudReactionEntry;
import com.bud.feature.bud.reaction.BudReactionKind;
import com.bud.feature.bud.reaction.LLMBudReactionMessageCreation;
import com.bud.feature.player.PlayerJoinSystem;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.feature.queue.state.StateChangeEntry;
import com.bud.feature.queue.state.StateChangeQueue;
import com.bud.feature.teleport.TeleportEvent;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.Pair;

public class BudCreationHandler implements Consumer<BudCreationEvent> {

    @Override
    public void accept(BudCreationEvent event) {
        if (!event.playerRef().isValid())
            return;
        World world = event.playerRef().getStore().getExternalData().getWorld();
        world.execute(() -> this.handleEvent(event));
    }

    private void handleEvent(BudCreationEvent event) {
        Store<EntityStore> store = event.playerRef().getStore();
        PlayerRef playerRef = store.getComponent(event.playerRef(), PlayerRef.getComponentType());
        if (playerRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid PlayerRef provided in BudCreationEvent.");
            return;
        }
        PlayerBudComponent playerBudComponent = store.getComponent(event.playerRef(),
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] PlayerBudComponent not found for player.");
            return;
        }
        List<BudComponent> existingBudTeleports = new ArrayList<>();
        Set<Vector3d> reservedPositions = new HashSet<>();
        for (NPCEntity existingBud : playerBudComponent.getCurrentBuds()) {
            Vector3d existingPosition = BudManager.getInstance().getBudPosition(Objects.requireNonNull(existingBud));
            if (existingPosition != null) {
                reservedPositions.add(existingPosition);
            }
            BudComponent budComponent = BudManager.getInstance().findBudComponent(existingBud);
            if (budComponent == null) {
                continue;
            }
            if (!event.budIds().contains(budComponent.getBudId())) {
                continue;
            }
            if (budComponent.getCurrentState() == BudState.WORKING) {
                // Stays at its workstation - it already counts as "owned" for the dedup check in
                // createBud(...) (BudManager.playerHasValidBud), so it won't be spawned a second time
                // either. See docs/bud-worker-mode-plan.md, "Working-State / Kampf-Lock".
                continue;
            }
            existingBudTeleports.add(budComponent);
        }

        int total = event.budIds().size();
        int spawnCounter = 0;
        for (String budId : event.budIds()) {
            int spawnIndex = spawnCounter++;
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Creating Bud of id " + budId);
            if (budId == null || !BudRegistry.getInstance().exists(budId)) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Invalid Bud id provided: " + budId);
                continue;

            }
            this.createBud(store, playerRef, budId, playerBudComponent, event.triggerGreetings(), spawnIndex, total,
                    reservedPositions);
        }

        if (!existingBudTeleports.isEmpty()) {
            int speakingIndex = ThreadLocalRandom.current().nextInt(existingBudTeleports.size());
            for (int index = 0; index < existingBudTeleports.size(); index++) {
                BudComponent budComponent = existingBudTeleports.get(index);
                if (budComponent != null) {
                    TeleportEvent.dispatch(store, budComponent, index == speakingIndex);
                }
            }
        }
    }

    private void createBud(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            @Nonnull String budId, @Nonnull PlayerBudComponent playerBudComponent, boolean triggerGreetings,
            int index, int total, @Nonnull Set<Vector3d> reservedPositions) {
        if (BudManager.playerHasValidBud(playerBudComponent, budId)) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Player already has Bud of id " + budId);
            return;
        }
        NPCEntity bud = spawnBud(store, playerRef, budId, index, total, reservedPositions);
        if (bud == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to spawn Bud of id " + budId);
            return;
        }
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Successfully spawned Bud with NPC Type ID: " + bud.getNPCTypeId());
        playerBudComponent.addBud(bud, budId);
        BudComponent budComponent = registerBudComponent(store, bud, playerRef, budId);
        if (budComponent == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Failed to register BudComponent for Bud of id " + budId);
            return;
        }
        StateChangeQueue.getInstance()
                .addToCache(new StateChangeEntry(BudState.PET_DEFENSIVE, budComponent));
        PlayerJoinSystem.initializeWeatherBaseline(playerRef, playerBudComponent);
        if (DebugConfig.getInstance().isEnableBudDebugInfo()) {
            BudDebugInfo.getInstance().logBudInfo(bud);
        }
        if (triggerGreetings) {
            triggerGreetingReaction(playerRef, playerBudComponent, budComponent, budId);
        }
    }

    private void triggerGreetingReaction(@Nonnull PlayerRef playerRef, @Nonnull PlayerBudComponent playerBudComponent,
            @Nonnull BudComponent newBudComponent, @Nonnull String newBudId) {
        BudComponent otherBud = BudManager.getInstance().getRandomOtherBud(playerBudComponent, newBudComponent);
        if (otherBud == null) {
            return;
        }
        BudDefinition newBudProfile = BudRegistry.getInstance().get(newBudId);
        String situationInfo = newBudProfile.getDisplayName() + " just joined the group. Greet them in character. "
                + newBudProfile.getPronounHint();
        BudReactionEntry entry = new BudReactionEntry(otherBud, BudReactionKind.GREETING, situationInfo);
        long now = System.currentTimeMillis();
        Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                OrchestratorChannel.SOCIAL,
                entry,
                entry.getEntryName() + ":" + now,
                playerRef.getUsername(),
                new LLMInteractionEntry(LLMBudReactionMessageCreation.getInstance(), entry),
                now));
    }

    private static NPCEntity spawnBud(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            @Nonnull String budId, int index, int total, @Nonnull Set<Vector3d> reservedPositions) {
        BudDefinition budProfile = BudRegistry.getInstance().get(budId);
        Vector3d position = BudManager.getInstance().getSpawnPosition(playerRef, index, total, reservedPositions);
        Vector3f rotation = BudManager.getInstance().getRotationFacingPlayer(playerRef, position);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = BudSpawner
                .create(store, budProfile.getNpcTypeId(), position)
                .withRotation(rotation)
                .withInventory()
                .addWeapon(budProfile.getWeaponId(), 1, (short) 0)
                .addArmor(budProfile.getArmorId())
                .spawn();
        return (NPCEntity) result.second();
    }

    private BudComponent registerBudComponent(@Nonnull Store<EntityStore> store, NPCEntity bud,
            @Nonnull PlayerRef playerRef, @Nonnull String budId) {
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Invalid NPCEntity reference for bud: " + bud);
            return null;
        }
        BudComponent budComponent = BudComponent.create(bud, budId, playerRef);
        store.addComponent(ref, BudComponent.getComponentType(), budComponent);
        return budComponent;
    }
}
