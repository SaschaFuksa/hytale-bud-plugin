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

public class WorkstationFilterSystem extends RefSystem<ChunkStore> {

        private static final short CARD_SLOT = 0;
        private static final short SECONDARY_INPUT_SLOT = 1;
        private static final short FEED_SLOT = 0;

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
                WorkstationBlockEntity workstation = commandBuffer.getComponent(ref,
                                WorkstationBlockEntity.getComponentType());
                ProcessingBenchBlock processingBenchBlock = commandBuffer.getComponent(ref,
                                WorkstationCardUtil.getProcessingBenchBlockComponentType());
                BenchBlock benchBlock = commandBuffer.getComponent(ref,
                                WorkstationCardUtil.getBenchBlockComponentType());
                if (workstation == null || processingBenchBlock == null || benchBlock == null) {
                        return;
                }
                if (processingBenchBlock.getInputContainer() == null
                                || processingBenchBlock.getFuelContainer() == null) {
                        LoggerUtil.getLogger().warning(
                                        () -> "[BUD] Workstation's Processing Bench containers were not yet initialized when "
                                                        + "WorkstationFilterSystem ran (addReason=" + addReason
                                                        + ") - card filter/binding not installed.");
                        return;
                }

                WorkRole workRole = workstation.getWorkRole();
                processingBenchBlock.getInputContainer().setSlotFilter(FilterActionType.ADD, CARD_SLOT,
                                (actionType, container, slot, itemStack) -> WorkstationCardUtil
                                                .matchesWorkRole(itemStack, workRole));
                processingBenchBlock.getInputContainer().setSlotFilter(FilterActionType.ADD, SECONDARY_INPUT_SLOT,
                                (actionType, container, slot,
                                                itemStack) -> WorkstationCardUtil.resolveBudId(itemStack) == null
                                                                && WorkstationSeedUtil.isAllowedSeed(itemStack,
                                                                                workRole));
                processingBenchBlock.getFuelContainer().setSlotFilter(FilterActionType.ADD, FEED_SLOT,
                                (actionType, container, slot,
                                                itemStack) -> WorkstationFuelUtil.isAllowedFuel(itemStack, workRole));
                processingBenchBlock.getInputContainer().registerChangeEvent(
                                event -> WorkstationBindingHandler.reevaluate(store, ref, workstation,
                                                processingBenchBlock, benchBlock));
                processingBenchBlock.getFuelContainer().registerChangeEvent(
                                event -> {
                                        WorkstationBindingHandler.reevaluate(store, ref, workstation,
                                                        processingBenchBlock, benchBlock);
                                        workstation.onFuelContainerChanged();
                                });
                if (processingBenchBlock.getOutputContainer() != null) {
                        processingBenchBlock.getOutputContainer()
                                        .registerChangeEvent(event -> workstation.onOutputContainerChanged());
                }

                WorkstationBindingHandler.reevaluate(store, ref, workstation, processingBenchBlock, benchBlock);
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason,
                        @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
                WorkstationBlockEntity workstation = store.getComponent(ref, WorkstationBlockEntity.getComponentType());
                if (workstation != null) {
                        WorkstationBindingHandler.despawnBoundBud(store, workstation);
                }
        }

}
