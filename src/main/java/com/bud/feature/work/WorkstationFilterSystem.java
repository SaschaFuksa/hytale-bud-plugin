package com.bud.feature.work;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.registry.BudRegistry;
import com.bud.core.types.WorkRole;
import com.bud.interaction.CardBudInteraction;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerBlockWindow;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterActionType;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/**
 * Rejects inserting a Bud card into a Workstation's card slot (slot 0) unless the card's Bud has a
 * matching {@link WorkRole}. Installed via {@link com.hypixel.hytale.server.core.inventory.container.filter.SlotFilter}
 * on the native {@link ItemContainerBlock}'s container - see docs/bud-worker-mode-plan.md, "Phase 3".
 */
public class WorkstationFilterSystem extends RefSystem<ChunkStore> {

    private static final short CARD_SLOT = 0;

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(WorkstationBlockEntity.getComponentType(), getItemContainerBlockComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        WorkstationBlockEntity workstation = store.getComponent(ref, WorkstationBlockEntity.getComponentType());
        ItemContainerBlock containerBlock = store.getComponent(ref, getItemContainerBlockComponentType());
        if (workstation == null || containerBlock == null) {
            return;
        }
        WorkRole workRole = workstation.getWorkRole();
        containerBlock.getItemContainer().setSlotFilter(FilterActionType.ADD, CARD_SLOT,
                (actionType, container, slot, itemStack) -> {
                    if (!matchesWorkRole(itemStack, workRole)) {
                        return false;
                    }
                    // Side effect inside a filter predicate is intentional here: this is the only hook
                    // that fires exactly when a valid card insert happens and has the ItemContainerBlock
                    // (needed for getWindows()) in scope - see docs/bud-worker-mode-plan.md, "Besitzer-Bindung".
                    assignOwner(workstation, containerBlock);
                    return true;
                });
    }

    /**
     * Ownership is (re-)assigned to whoever performs the latest accepted card insert - there is no lock
     * once a Workstation has an owner. A second player replacing the card becomes the new owner; this is
     * a deliberately simple placeholder (no access control yet, see docs/bud-worker-mode-plan.md,
     * "Besitzer-Bindung") - Phase 4, once the actual Bud binding/fuel logic lands, should decide whether
     * an owned Workstation needs to reject card changes from non-owners instead.
     */
    private static void assignOwner(@Nonnull WorkstationBlockEntity workstation,
            @Nonnull ItemContainerBlock containerBlock) {
        UUID playerId = resolveSoleViewer(containerBlock);
        if (playerId != null) {
            workstation.setOwnerPlayerId(playerId);
        }
    }

    /**
     * The container/slot-filter APIs don't carry the acting player (see docs/bud-worker-mode-plan.md,
     * "Besitzer-Bindung" for the full research trail) - the only place the player is available is the
     * open {@link ContainerBlockWindow}. Resolves the player only when exactly one player currently has
     * this Workstation's window open; ambiguous otherwise (nobody or several viewers at once), in which
     * case ownership is left unchanged rather than risk misattributing it.
     */
    @Nullable
    private static UUID resolveSoleViewer(@Nonnull ItemContainerBlock containerBlock) {
        Map<UUID, ContainerBlockWindow> windows = containerBlock.getWindows();
        if (windows.size() != 1) {
            return null;
        }
        PlayerRef playerRef = windows.values().iterator().next().getPlayerRef();
        return playerRef != null ? playerRef.getUuid() : null;
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        // No cleanup needed - the slot filter lives on the container instance, discarded with the block.
    }

    @Nonnull
    private static ComponentType<ChunkStore, ItemContainerBlock> getItemContainerBlockComponentType() {
        return Objects.requireNonNull(ItemContainerBlock.getComponentType());
    }

    private static boolean matchesWorkRole(@Nullable ItemStack itemStack, @Nonnull WorkRole workRole) {
        String budId = resolveBudId(itemStack);
        if (budId == null || !BudRegistry.getInstance().exists(budId)) {
            return false;
        }
        return BudRegistry.getInstance().get(budId).getWorkRole() == workRole;
    }

    @Nullable
    private static String resolveBudId(@Nullable ItemStack itemStack) {
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
