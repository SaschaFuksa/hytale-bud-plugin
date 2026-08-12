package com.bud.feature.work;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.components.BudComponent;
import com.bud.core.config.WorkConfig;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.npc.role.Role;

public class WorkstationFuelTickSystem extends EntityTickingSystem<ChunkStore> {

    private static final short CARD_SLOT = 0;
    private static final short FEED_SLOT = 0;

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(WorkstationBlockEntity.getComponentType(),
                WorkstationCardUtil.getProcessingBenchBlockComponentType(),
                WorkstationCardUtil.getBenchBlockComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        WorkstationBlockEntity workstation = archetypeChunk.getComponent(index,
                WorkstationBlockEntity.getComponentType());
        if (workstation == null) {
            return;
        }
        ProcessingBenchBlock processingBenchBlock = archetypeChunk.getComponent(index,
                WorkstationCardUtil.getProcessingBenchBlockComponentType());
        if (processingBenchBlock == null || processingBenchBlock.getFuelContainer() == null
                || processingBenchBlock.getInputContainer() == null) {
            return;
        }

        BudComponent boundBud = workstation.getBoundBud();
        if (boundBud == null) {
            // Persistence across relog/restart (Option B, see docs/bud-worker-mode-plan.md,
            // "Persistenz
            // über Relog/Neustart"): the initial bind attempt in
            // WorkstationFilterSystem.onEntityAdded runs
            // once when the block loads, but if the owner isn't online yet at that exact
            // moment, bind()
            // skips gracefully and nothing else would ever retry it. Every Workstation is
            // already ticked
            // here regardless of binding state, so a throttled retry piggybacks on the
            // existing tick
            // instead of a new "all stations of a player" lookup.
            tryRebind(store, archetypeChunk, index, workstation, processingBenchBlock, dt);
            return;
        }

        boolean pausedByBench = !processingBenchBlock.isActive();
        // Visual sub-state (see docs/bud-worker-mode-plan.md, "Working/Resting
        // Sub-States") reflects either
        // pause reason uniformly - computed and applied once per tick, before any early
        // return below, so
        // both "out of fuel" and "Bench manually turned off" look the same to the
        // player (Sascha: "Bud dann
        // sinnvollerweise wie im Resting-Zustand behandeln"). Safe to call every tick -
        // StateSupport.
        // setSubState(...) just re-applies setState(...) with the same index when
        // already in that sub-state,
        // a harmless no-op re-set (bytecode-verified).
        setRestingSubState(boundBud, pausedByBench || workstation.isResting());

        if (pausedByBench) {
            // Player toggled the native Processing Bench "TURN OFF" control (protocol-level
            // SetActiveAction -> ProcessingBenchBlock.setActive(...), bytecode-verified -
            // see
            // docs/bud-worker-mode-plan.md, "TURN OFF-Knopf"). Pause entirely: no
            // countdown, no fuel
            // consumption, no resume-from-rest attempt. Deliberately distinct from
            // workstation.isResting() (out of fuel, costs a fresh serving to resume) -
            // being paused this
            // way costs nothing to resume, the countdown just picks up where it left off
            // once turned back
            // on. bind() defaults a freshly bound Workstation to "on".
            return;
        }

        if (workstation.isResting()) {
            tryResumeFromRest(workstation, boundBud, processingBenchBlock);
            return;
        }

        float remaining = workstation.getFuelSecondsRemaining() - dt;
        if (remaining > 0) {
            workstation.setFuelSecondsRemaining(remaining);
            return;
        }
        if (consumeOneFuel(processingBenchBlock)) {
            workstation.setFuelSecondsRemaining(WorkConfig.getInstance().getFuelDurationSeconds());
            return;
        }
        workstation.setFuelSecondsRemaining(0f);
        workstation.setResting(true);
        String restingBudId = boundBud.getBudId();
        LoggerUtil.getLogger().info(() -> "[BUD] Workstation out of fuel, Bud " + restingBudId + " is resting.");
    }

    private static void tryRebind(@Nonnull Store<ChunkStore> store, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
            int index, @Nonnull WorkstationBlockEntity workstation, @Nonnull ProcessingBenchBlock processingBenchBlock,
            float dt) {
        ItemStack card = processingBenchBlock.getInputContainer().getItemStack(CARD_SLOT);
        if (!WorkstationCardUtil.matchesWorkRole(card, workstation.getWorkRole())) {
            workstation.setRebindRetrySecondsRemaining(0f);
            return;
        }
        float remaining = workstation.getRebindRetrySecondsRemaining() - dt;
        if (remaining > 0) {
            workstation.setRebindRetrySecondsRemaining(remaining);
            return;
        }
        workstation.setRebindRetrySecondsRemaining(WorkConfig.getInstance().getRebindRetrySeconds());
        BenchBlock benchBlock = archetypeChunk.getComponent(index, WorkstationCardUtil.getBenchBlockComponentType());
        if (benchBlock == null) {
            return;
        }
        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        WorkstationBindingHandler.reevaluate(store, ref, workstation, processingBenchBlock, benchBlock);
    }

    private static void tryResumeFromRest(@Nonnull WorkstationBlockEntity workstation, @Nonnull BudComponent boundBud,
            @Nonnull ProcessingBenchBlock processingBenchBlock) {
        if (!consumeOneFuel(processingBenchBlock)) {
            return;
        }
        workstation.setResting(false);
        workstation.setFuelSecondsRemaining(WorkConfig.getInstance().getFuelDurationSeconds());
        String resumingBudId = boundBud.getBudId();
        LoggerUtil.getLogger().info(() -> "[BUD] Workstation refed, Bud " + resumingBudId + " resumes work.");
    }

    private static boolean consumeOneFuel(@Nonnull ProcessingBenchBlock processingBenchBlock) {
        ItemStack fuel = processingBenchBlock.getFuelContainer().getItemStack(FEED_SLOT);
        if (isEmpty(fuel)) {
            return false;
        }
        processingBenchBlock.getFuelContainer().removeItemStackFromSlot(FEED_SLOT, 1);
        return true;
    }

    private static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.isEmpty();
    }

    /**
     * Drives the Working state's ".Resting"/".Default" role sub-states directly via
     * {@code StateSupport.setSubState(String)} - bypasses
     * {@code BudState}/{@code StateChangeEvent} entirely
     * (this is a purely visual, finer-grained concept nested inside the
     * already-active {@code WORKING}
     * top-level state, not a new {@code BudState} value). Bytecode-verified (see
     * docs/bud-worker-mode-plan.md, "Working/Resting Sub-States"): the sub-state
     * name passed to
     * {@code setSubState}/{@code getSubStateIndex} is the bare name without the
     * JSON's leading dot (the dot
     * is JSON-authoring convention only, stripped before use as an internal map key
     * - confirmed via the
     * engine's own default-sub-state fallback literal, {@code "Default"} with no
     * dot). Silently no-ops if
     * the Bud's role has no matching sub-state registered (e.g. an older/unmigrated
     * role JSON) rather than
     * throwing - same graceful-skip style as the rest of this feature.
     */
    private static void setRestingSubState(@Nonnull BudComponent boundBud, boolean resting) {
        Role role = boundBud.getBud().getRole();
        if (role == null) {
            return;
        }
        role.getStateSupport().setSubState(resting ? "Resting" : "Default");
    }

}
