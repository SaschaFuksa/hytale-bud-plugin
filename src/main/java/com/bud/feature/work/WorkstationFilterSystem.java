package com.bud.feature.work;

import javax.annotation.Nonnull;

import com.bud.core.types.WorkRole;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Rejects inserting a Bud card into a Workstation's card slot (input slot 0 of the native Processing Bench)
 * unless the card's Bud has a matching {@link WorkRole}, and rejects Bud cards outright from the second
 * Input slot (reserved for a future Seedbag filter, Phase 5/8 - not typed yet, but must not accept a card
 * either). Installed via {@link com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter} on
 * {@link ProcessingBenchBlock#getInputContainer()} - same {@code setSlotFilter(FilterActionType.ADD, ...)}
 * pattern the engine itself uses for e.g. armor slots ({@code ArmorSlotAddFilter}), bytecode-verified to
 * actually gate player drag-and-drop inserts via {@code cantAddToSlot} - see docs/bud-worker-mode-plan.md,
 * "Vorab-Frage 2". Also registers the container change listeners that drive Bud binding/unbinding
 * (Phase 4) - see {@link WorkstationBindingHandler}.
 */
public class WorkstationFilterSystem extends RefSystem<ChunkStore> {

    private static final short CARD_SLOT = 0;
    // The Bench's second Input slot (reserved for a future Seedbag filter, Phase 5/8) - not typed yet, but
    // must reject Bud cards in the meantime so a card can't be pushed in there instead of Slot 0 - see
    // docs/bud-worker-mode-plan.md, "Zweiter Input-Slot ungefiltert".
    private static final short SECONDARY_INPUT_SLOT = 1;

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(WorkstationBlockEntity.getComponentType(),
                WorkstationCardUtil.getProcessingBenchBlockComponentType(),
                WorkstationCardUtil.getBenchBlockComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        // Must read via the CommandBuffer, not the Store, here: for a freshly placed block (AddReason.SPAWN)
        // the just-added components aren't committed to the Store yet at this point in the lifecycle - the
        // engine's own equivalent (ItemContainerSystems$OnAddedOrRemoved) does the same, bytecode-verified.
        // See docs/bud-worker-mode-plan.md, "Regression 1: Slot 1 (Futter) ließ sich nicht einlegen".
        WorkstationBlockEntity workstation = commandBuffer.getComponent(ref, WorkstationBlockEntity.getComponentType());
        ProcessingBenchBlock processingBenchBlock = commandBuffer.getComponent(ref,
                WorkstationCardUtil.getProcessingBenchBlockComponentType());
        BenchBlock benchBlock = commandBuffer.getComponent(ref, WorkstationCardUtil.getBenchBlockComponentType());
        if (workstation == null || processingBenchBlock == null || benchBlock == null) {
            return;
        }
        // The native BenchSystems$ProcessingBenchLifecycle$OnAddOrRemoved system (registered by the engine's
        // own crafting plugin, long before ours) is what actually builds inputContainer/fuelContainer via
        // ProcessingBenchBlock.setupSlots(...) - until that has run, both getters return null. Engine-core
        // systems register before plugin systems in the bootstrap order, so by the time this fires the
        // containers are expected to already exist; guard defensively and log instead of crashing if not.
        if (processingBenchBlock.getInputContainer() == null || processingBenchBlock.getFuelContainer() == null) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] Workstation's Processing Bench containers were not yet initialized when "
                            + "WorkstationFilterSystem ran (addReason=" + addReason + ") - card filter/binding not installed.");
            return;
        }

        WorkRole workRole = workstation.getWorkRole();
        processingBenchBlock.getInputContainer().setSlotFilter(FilterActionType.ADD, CARD_SLOT,
                (actionType, container, slot, itemStack) -> WorkstationCardUtil.matchesWorkRole(itemStack, workRole));
        processingBenchBlock.getInputContainer().setSlotFilter(FilterActionType.ADD, SECONDARY_INPUT_SLOT,
                (actionType, container, slot, itemStack) -> WorkstationCardUtil.resolveBudId(itemStack) == null);
        processingBenchBlock.getInputContainer().registerChangeEvent(
                event -> WorkstationBindingHandler.reevaluate(store, ref, workstation, processingBenchBlock, benchBlock));
        processingBenchBlock.getFuelContainer().registerChangeEvent(
                event -> WorkstationBindingHandler.reevaluate(store, ref, workstation, processingBenchBlock, benchBlock));

        // Persistence across relog/restart (Option B, see docs/bud-worker-mode-plan.md, "Persistenz über
        // Relog/Neustart"): the card/fuel slot contents already survive a restart natively
        // (ProcessingBenchBlock's own CODEC persists InputContainer/FuelContainer), but the binding itself
        // (WorkstationBlockEntity.boundBud) is transient and BudComponent isn't persisted at all - so a
        // reloaded Workstation with a card still in it has no bound Bud until something re-derives the
        // binding from that persisted card content. Do that once here for both AddReason.LOAD (the actual
        // restart-recovery case) and AddReason.SPAWN (harmless no-op, slots are empty on a freshly placed
        // block). If the owner isn't online yet at this exact moment, bind() skips gracefully and
        // WorkstationFuelTickSystem's throttled retry (see there) picks it up once they log back in.
        WorkstationBindingHandler.reevaluate(store, ref, workstation, processingBenchBlock, benchBlock);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        // The slot filter/change listeners themselves need no cleanup - they live on the container
        // instances, discarded with the block. But if a Bud is still bound when the Workstation block is
        // destroyed/unloaded, it must not be left dangling in WORKING forever. Unified semantic (see
        // docs/bud-worker-mode-plan.md, "Station abbauen: ein Weg statt zwei"): the block breaking also
        // ejects the card, so this is treated exactly like an explicit card removal - despawnBoundBud - not
        // a separate "restore previous mode, keep the Bud" special case anymore. Read via the Store, not
        // the CommandBuffer, here - the component is still committed at this point, removal hasn't happened
        // yet (same accessor pattern as e.g. TeleportFilterSystem.onComponentRemoved).
        WorkstationBlockEntity workstation = store.getComponent(ref, WorkstationBlockEntity.getComponentType());
        if (workstation != null) {
            WorkstationBindingHandler.despawnBoundBud(store, workstation);
        }
    }

}
