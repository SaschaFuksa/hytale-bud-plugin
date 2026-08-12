package com.bud.feature.work;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.WorkConfig;
import com.bud.core.registry.BudDefinition;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.BudState;
import com.bud.feature.bud.creation.BudSpawner;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.state.StateChangeEvent;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.crafting.window.BenchWindow;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import it.unimi.dsi.fastutil.Pair;

/**
 * Binds/unbinds a Bud to a Workstation based on the current card (Processing Bench input slot 0) content -
 * fuel no longer gates binding, only work-vs-resting once bound (Design-Änderung, Sascha) - see
 * docs/bud-worker-mode-plan.md, "Bindung ohne Fuel-Bedingung (Design-Änderung)". Invoked from
 * {@link WorkstationFilterSystem}'s container change listeners on every input/fuel change.
 */
final class WorkstationBindingHandler {

    private static final short CARD_SLOT = 0;
    private static final short FEED_SLOT = 0;
    private static final int STATION_FRONT_DISTANCE = 1;

    private WorkstationBindingHandler() {
    }

    static void reevaluate(@Nonnull Store<ChunkStore> store, @Nonnull Ref<ChunkStore> ref,
            @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            @Nonnull BenchBlock benchBlock) {
        ItemStack card = processingBenchBlock.getInputContainer().getItemStack(CARD_SLOT);
        boolean hasValidCard = WorkstationCardUtil.matchesWorkRole(card, workstation.getWorkRole());

        // Unlike the earlier ItemContainerBlock/chest-primitive attempt, an invalid card is now rejected at
        // the source: the SlotFilter installed in WorkstationFilterSystem gates cantAddToSlot, which the
        // real player drag-and-drop move path (InventoryUtils.moveItem -> moveItemStackFromSlotToSlot)
        // does consult - bytecode-verified, see docs/bud-worker-mode-plan.md, "Vorab-Frage 2". No reactive
        // eviction is needed here anymore.

        if (hasValidCard) {
            assignOwner(workstation, benchBlock);
        }

        if (workstation.getBoundBud() == null) {
            // Design change (Sascha): a valid card alone spawns/binds the Bud - fuel only decides
            // working-vs-resting once bound, it no longer gates the bind itself. See
            // docs/bud-worker-mode-plan.md, "Bindung ohne Fuel-Bedingung (Design-Änderung)".
            if (hasValidCard) {
                bind(store, ref, workstation, processingBenchBlock, benchBlock, Objects.requireNonNull(card));
            }
        } else if (!hasValidCard) {
            // Design change (Sascha): the card "contains" the Bud again once removed/swapped for an invalid
            // one - unlike the earlier Phase 4 behaviour (restore previousState, Bud stays in the world),
            // taking the card out now despawns the Bud, same as `/bud delete`. See
            // docs/bud-worker-mode-plan.md, "Karte raus despawnt den Bud (Design-Änderung)".
            despawnBoundBud(store, workstation);
        }
    }

    /**
     * Ownership is (re-)assigned to whoever currently has a valid card in the input slot - there is no lock
     * once a Workstation has an owner, matching the deliberately simple placeholder from Phase 3 (no access
     * control yet, see docs/bud-worker-mode-plan.md, "Besitzer-Bindung").
     */
    private static void assignOwner(@Nonnull WorkstationBlockEntity workstation, @Nonnull BenchBlock benchBlock) {
        PlayerRef viewer = resolveSoleViewer(benchBlock);
        if (viewer != null) {
            workstation.setOwnerPlayerId(viewer.getUuid());
        }
    }

    /**
     * "Exactly one current viewer" heuristic - {@link BenchBlock#getWindows()} is the direct engine-provided
     * map of currently-open windows for this block (bytecode-verified structurally identical to
     * {@code ItemContainerBlock.getWindows()} used by the earlier chest-based attempt), no more direct
     * single-actor hook exists on the Processing Bench window either - see docs/bud-worker-mode-plan.md,
     * "Besitzer-Bindung" for the reasoning.
     */
    @Nullable
    private static PlayerRef resolveSoleViewer(@Nonnull BenchBlock benchBlock) {
        Map<UUID, BenchWindow> windows = benchBlock.getWindows();
        if (windows.size() != 1) {
            return null;
        }
        return windows.values().iterator().next().getPlayerRef();
    }

    /**
     * Validates synchronously (owner recorded, card resolves to a known Bud) and, only if that passes,
     * defers the actual entity mutation (spawn/teleport, {@code addComponent}) to {@link #performBind} via
     * {@link World#execute(Runnable)} - the ECS {@code Store} forbids reentrant mutation while it is
     * mid-processing a system ("IllegalStateException: Store is currently processing! Ensure you aren't
     * calling a store method from a system."), and every caller of {@link #reevaluate} runs from inside
     * such a system callback (container change listener, {@code onEntityAdded}, the fuel-tick's rebind
     * retry). {@code World.execute(...)} is the same task-queue deferral {@code CleanupUtil} already uses
     * for {@code /bud delete} - bytecode-verified to unconditionally enqueue rather than ever run inline,
     * and drained at safe (non-reentrant) points in the world tick. See docs/bud-worker-mode-plan.md,
     * "'Store is currently processing' - Entity-Operationen aus System-Callbacks verlagert".
     */
    private static void bind(@Nonnull Store<ChunkStore> chunkStore, @Nonnull Ref<ChunkStore> ref,
            @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            @Nonnull BenchBlock benchBlock, @Nonnull ItemStack card) {
        UUID ownerId = workstation.getOwnerPlayerId();
        if (ownerId == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Workstation has a valid card but no owner recorded - skipping bind.");
            return;
        }
        String budId = WorkstationCardUtil.resolveBudId(card);
        if (budId == null) {
            return;
        }
        World world = chunkStore.getExternalData().getWorld();
        world.execute(() -> performBind(chunkStore, world, ref, workstation, processingBenchBlock, benchBlock,
                ownerId, budId));
    }

    private static void performBind(@Nonnull Store<ChunkStore> chunkStore, @Nonnull World world,
            @Nonnull Ref<ChunkStore> ref, @Nonnull WorkstationBlockEntity workstation,
            @Nonnull ProcessingBenchBlock processingBenchBlock, @Nonnull BenchBlock benchBlock,
            @Nonnull UUID ownerId, @Nonnull String budId) {
        if (!ref.isValid() || workstation.getBoundBud() != null) {
            // Station gone, or a concurrently-enqueued reevaluate() already bound it by the time this runs.
            return;
        }
        Store<EntityStore> entityStore = Objects.requireNonNull(world.getEntityStore().getStore());

        PlayerBudComponent ownerBuds = resolveOwnerPlayerBudComponent(entityStore, ownerId);
        if (ownerBuds == null) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] Owner of Workstation is not currently online/resolvable - skipping bind for now.");
            return;
        }
        Vector3d position = resolveSpawnPositionInFrontOfStation(chunkStore, world, ref);
        if (position == null) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Could not resolve Workstation world position - skipping bind.");
            return;
        }

        BudComponent existing = findOwnedBud(ownerBuds, budId);
        BudComponent bound = existing != null
                ? teleportToStation(entityStore, existing, position)
                : spawnAtStation(entityStore, ownerBuds, budId, position);
        if (bound == null) {
            final String failedBudId = budId;
            LoggerUtil.getLogger().severe(() -> "[BUD] Failed to bind Bud '" + failedBudId + "' to Workstation.");
            return;
        }

        bound.setCurrentState(BudState.WORKING);
        StateChangeEvent.dispatch(bound.getBud(), bound.getPlayerRef(), BudState.WORKING);

        workstation.setBoundBud(bound);
        // Design change (Sascha): binding no longer requires fuel to be present - a Bud spawns as soon as a
        // valid card is inserted. Without fuel it starts directly in the resting state instead (fuel only
        // decides working-vs-resting from here on); WorkstationFuelTickSystem's per-tick
        // pausedByBench||isResting() check picks up the visual sub-state within one tick either way. See
        // docs/bud-worker-mode-plan.md, "Bindung ohne Fuel-Bedingung (Design-Änderung)".
        boolean hasFuel = !isEmptyStack(processingBenchBlock.getFuelContainer().getItemStack(FEED_SLOT));
        workstation.setFuelSecondsRemaining(hasFuel ? WorkConfig.getInstance().getFuelDurationSeconds() : 0f);
        workstation.setResting(!hasFuel);

        // Binding defaults the native Processing Bench "TURN OFF"/"TURN ON" toggle (protocol-level
        // SetActiveAction -> ProcessingBenchBlock.setActive(...), bytecode-verified) to "on" - it defaults
        // to Java's `false` otherwise (never auto-activated for a bench with a fuel slot, since recipe is
        // always null here, see docs/bud-worker-mode-plan.md, "Vorab-Frage 1, Korrektur"), which would mean
        // WorkstationFuelTickSystem's isActive() gate (see there) pauses fuel consumption immediately after
        // every bind unless a player happens to have opened the window and flipped it on manually.
        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref,
                Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()));
        if (blockStateInfo != null) {
            processingBenchBlock.setActive(true, benchBlock, blockStateInfo);
        }
    }

    /**
     * Card taken out of a Workstation (still exists or is itself being destroyed/unloaded) - the card
     * "contains" the Bud, so the Bud is despawned. One unified semantic for both triggers (see
     * docs/bud-worker-mode-plan.md, "Station abbauen: ein Weg statt zwei" for why the earlier
     * card-removal-vs-station-destroyed split was replaced by this single path), called from both
     * {@link #reevaluate} (card removed/swapped while the Workstation still exists) and
     * {@link WorkstationFilterSystem#onEntityRemove} (Workstation block itself destroyed/unloaded while
     * still bound). Idempotent - {@code workstation.getBoundBud() == null} short-circuits, so it is safe to
     * call from both if both happen to fire for the same removal. The {@code WorkstationBlockEntity} fields
     * are cleared immediately (plain POJO writes, no Store involved) so a second concurrent call sees
     * "already unbound" right away; only the actual entity mutation is deferred - see {@link #bind} for why.
     */
    static void despawnBoundBud(@Nonnull Store<ChunkStore> chunkStore, @Nonnull WorkstationBlockEntity workstation) {
        BudComponent bud = workstation.getBoundBud();
        if (bud == null) {
            return;
        }
        workstation.setBoundBud(null);
        workstation.setFuelSecondsRemaining(0f);
        workstation.setResting(false);

        World world = chunkStore.getExternalData().getWorld();
        world.execute(() -> performDespawn(world, bud));
    }

    private static void performDespawn(@Nonnull World world, @Nonnull BudComponent bud) {
        Store<EntityStore> entityStore = Objects.requireNonNull(world.getEntityStore().getStore());
        NPCEntity npc = bud.getBud();
        PlayerRef ownerPlayerRef = bud.getPlayerRef();
        Ref<EntityStore> ownerEntityRef = ownerPlayerRef.getReference();
        if (ownerEntityRef != null) {
            PlayerBudComponent playerBudComponent = entityStore.getComponent(ownerEntityRef,
                    PlayerBudComponent.getComponentType());
            if (playerBudComponent != null) {
                playerBudComponent.removeCurrentBud(npc, bud.getBudId());
            }
        }
        Ref<EntityStore> budRef = npc.getReference();
        if (budRef != null && budRef.isValid()) {
            entityStore.removeEntity(budRef, RemoveReason.REMOVE);
        }
        Orchestrator.getInstance().purgeBud(ownerPlayerRef.getUsername(), bud.getBudId());
    }

    @Nullable
    private static PlayerBudComponent resolveOwnerPlayerBudComponent(@Nonnull Store<EntityStore> entityStore,
            @Nonnull UUID ownerId) {
        World world = entityStore.getExternalData().getWorld();
        for (PlayerBudComponent candidate : BudManager.getInstance().getAllPlayers(world)) {
            if (ownerId.equals(candidate.getPlayerRef().getUuid())) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private static BudComponent findOwnedBud(@Nonnull PlayerBudComponent ownerBuds, @Nonnull String budId) {
        for (NPCEntity bud : ownerBuds.getCurrentBuds()) {
            BudComponent component = BudManager.getInstance().findBudComponent(bud);
            if (component != null && component.getBudId().equals(budId)) {
                return component;
            }
        }
        return null;
    }

    @Nullable
    private static BudComponent spawnAtStation(@Nonnull Store<EntityStore> entityStore,
            @Nonnull PlayerBudComponent ownerBuds, @Nonnull String budId, @Nonnull Vector3d position) {
        BudDefinition budProfile = BudRegistry.getInstance().get(budId);
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = BudSpawner
                .create(entityStore, budProfile.getNpcTypeId(), position)
                .withInventory()
                .addWeapon(budProfile.getWeaponId(), 1, (short) 0)
                .addArmor(budProfile.getArmorId())
                .spawn();
        if (result == null || result.first() == null) {
            return null;
        }
        NPCEntity bud = (NPCEntity) result.second();
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null) {
            return null;
        }
        ownerBuds.addBud(bud, budId);
        BudComponent component = BudComponent.create(bud, budId, ownerBuds.getPlayerRef());
        entityStore.addComponent(ref, BudComponent.getComponentType(), component);
        return component;
    }

    /**
     * Silent remove+respawn at a fixed position, same pattern as {@code TeleportHandler.teleportBud} but
     * without its delay/`TeleportQueue` reaction dispatch - see docs/bud-worker-mode-plan.md, "LLM-Reaktionen
     * (bewusst zurückgestellt)": no chat/sound when a Bud is sent to work.
     */
    @Nullable
    private static BudComponent teleportToStation(@Nonnull Store<EntityStore> entityStore,
            @Nonnull BudComponent budComponent, @Nonnull Vector3d position) {
        NPCEntity oldBud = budComponent.getBud();
        Ref<EntityStore> oldRef = oldBud.getReference();
        if (oldRef == null || !oldRef.isValid()) {
            return null;
        }
        PlayerRef playerRef = budComponent.getPlayerRef();
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null) {
            return null;
        }
        PlayerBudComponent playerBudComponent = entityStore.getComponent(playerEntityRef,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            return null;
        }

        entityStore.removeEntity(oldRef, RemoveReason.REMOVE);
        playerBudComponent.removeCurrentBud(oldBud, budComponent.getBudId());

        BudDefinition budProfile = BudRegistry.getInstance().get(budComponent.getBudId());
        Pair<Ref<EntityStore>, INonPlayerCharacter> result = BudSpawner
                .create(entityStore, budProfile.getNpcTypeId(), position)
                .withInventory()
                .addWeapon(budProfile.getWeaponId(), 1, (short) 0)
                .addArmor(budProfile.getArmorId())
                .spawn();
        if (result == null || result.first() == null) {
            return null;
        }
        NPCEntity newBud = Objects.requireNonNull((NPCEntity) result.second());
        Ref<EntityStore> newRef = Objects.requireNonNull(result.first());
        budComponent.setBud(newBud);
        entityStore.addComponent(newRef, BudComponent.getComponentType(), budComponent);
        playerBudComponent.addBud(newBud, budComponent.getBudId());
        return budComponent;
    }

    /**
     * Derives a spawn position {@link #STATION_FRONT_DISTANCE} block(s) in front of the Workstation,
     * facing away from it, instead of directly on top of it (Sascha: "Sie soll davor stehen"). Reuses
     * {@link BudManager#findFreeLateralPosition} (the same lateral free-space search used for
     * player-relative spawn fanning, made generic/public for exactly this reuse) rather than duplicating
     * it. Facing is derived from the block's own {@code VariantRotation} (native
     * {@code BlockSection.getRotationIndex(...)}, same accessor the engine's own Bench tick uses
     * internally) instead of a player's - see docs/bud-worker-mode-plan.md, "Spawn-Position vor der
     * Station".
     */
    @Nullable
    private static Vector3d resolveSpawnPositionInFrontOfStation(@Nonnull Store<ChunkStore> chunkStore,
            @Nonnull World world, @Nonnull Ref<ChunkStore> ref) {
        BlockModule.BlockStateInfo blockStateInfo = chunkStore.getComponent(ref,
                Objects.requireNonNull(BlockModule.BlockStateInfo.getComponentType()));
        if (blockStateInfo == null) {
            return null;
        }
        Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
        if (chunkRef == null || !chunkRef.isValid()) {
            return null;
        }
        BlockChunk blockChunk = chunkStore.getComponent(chunkRef, Objects.requireNonNull(BlockChunk.getComponentType()));
        if (blockChunk == null) {
            return null;
        }
        int index = blockStateInfo.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int localY = ChunkUtil.yFromBlockInColumn(index);
        int localZ = ChunkUtil.zFromBlockInColumn(index);
        int worldX = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getX(), localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(blockChunk.getZ(), localZ);
        Vector3d stationGroundPos = new Vector3d(worldX + 0.5, localY + 1.0, worldZ + 0.5);

        // getSectionAtBlockY is marked @Deprecated but is the same method the engine's own
        // BenchSystems$ProcessingBenchTick uses internally (bytecode-verified) - no non-deprecated
        // equivalent taking a block Y (only a raw section-index overload, which would need an unverified
        // section-height constant to convert into), so kept rather than guessed around.
        BlockSection section = blockChunk.getSectionAtBlockY(localY);
        int rotationIndex = section != null ? section.getRotationIndex(localX, localY, localZ) : 0;
        Rotation[] rotations = Rotation.values();
        Rotation rotation = rotations[Math.floorMod(rotationIndex, rotations.length)];
        float yaw = (float) rotation.getRadians();
        Vector3d forward = Objects.requireNonNull(Transform.getDirection(0f, yaw));
        Vector3d right = Objects.requireNonNull(Transform.getDirection(0f, yaw - (float) (Math.PI / 2)));

        Vector3d candidate = BudManager.findFreeLateralPosition(world, stationGroundPos, forward, right,
                STATION_FRONT_DISTANCE, 0.0, new HashSet<>());
        if (candidate != null) {
            return candidate;
        }
        // Best-effort fallback if the small lateral search found nothing free (e.g. boxed in) - still
        // better than the old "spawn inside the station" behaviour it replaces.
        return new Vector3d(stationGroundPos.x + forward.x * STATION_FRONT_DISTANCE, stationGroundPos.y,
                stationGroundPos.z + forward.z * STATION_FRONT_DISTANCE);
    }

    private static boolean isEmptyStack(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

}
