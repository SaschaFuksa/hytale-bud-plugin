package com.bud.feature.work;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.registry.BudRegistry;
import com.bud.core.types.WorkRole;
import com.bud.interaction.CardBudInteraction;
import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Shared Bud-card helpers used by both {@link WorkstationFilterSystem} (Slot-0 insert validation) and
 * {@link WorkstationBindingHandler} (bind/unbind on any container change) - see
 * docs/bud-worker-mode-plan.md, "Phase 3 — Verifikationsergebnisse", Punkt 3.
 */
final class WorkstationCardUtil {

    private WorkstationCardUtil() {
    }

    @Nonnull
    static ComponentType<ChunkStore, ProcessingBenchBlock> getProcessingBenchBlockComponentType() {
        return Objects.requireNonNull(ProcessingBenchBlock.getComponentType());
    }

    @Nonnull
    static ComponentType<ChunkStore, BenchBlock> getBenchBlockComponentType() {
        return Objects.requireNonNull(BenchBlock.getComponentType());
    }

    static boolean matchesWorkRole(@Nullable ItemStack itemStack, @Nonnull WorkRole workRole) {
        String budId = resolveBudId(itemStack);
        if (budId == null || !BudRegistry.getInstance().exists(budId)) {
            return false;
        }
        return BudRegistry.getInstance().get(budId).getWorkRole() == workRole;
    }

    @Nullable
    static String resolveBudId(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }
        String interactionId = itemStack.getItem().getInteractions().get(InteractionType.Primary);
        if (interactionId == null) {
            return null;
        }
        Interaction interaction = Interaction.getAssetMap().getAsset(interactionId);
        if (!(interaction instanceof CardBudInteraction cardBudInteraction)) {
            return null;
        }
        return cardBudInteraction.getBudId();
    }

}
