package com.bud.interaction;

import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.types.BudType;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.bud.feature.sound.SoundEvent;
import com.bud.feature.util.CleanupUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public abstract class CardBudInteraction extends SimpleInteraction {

    @Nonnull
    private final BudType budType;

    protected CardBudInteraction(String name, @Nonnull BudType budType) {
        super(name);
        this.budType = budType;
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        if (firstRun) {
            Ref<EntityStore> owningEntityRef = context.getOwningEntity();
            if (owningEntityRef == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] Owning entity not present for " + budType);
                return;
            }
            Store<EntityStore> store = owningEntityRef.getStore();
            PlayerRef playerRef = store.getComponent(owningEntityRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] PlayerRef not present for " + budType);
                return;
            }
            @Nonnull
            @SuppressWarnings("null")
            Set<BudType> budTypes = Set.of(budType);
            if (type == InteractionType.Primary) {
                LoggerUtil.getLogger()
                        .info(() -> "[BUD] Spawning " + budType + " for " + playerRef.getUsername());
                SoundEvent.dispatch(owningEntityRef, "SFX_Deployable_Totem_Heal_Spawn");
                BudCreationEvent.dispatch(owningEntityRef, budTypes);
            } else if (type == InteractionType.Secondary) {
                LoggerUtil.getLogger()
                        .info(() -> "[BUD] Despawning " + budType + " for " + playerRef.getUsername());
                SoundEvent.dispatch(owningEntityRef, "SFX_Deployable_Totem_Heal_Despawn");
                CleanupUtil.cleanupBuds(playerRef, store, budTypes);
            }
        }
        super.tick0(firstRun, time, type, context, cooldownHandler);
    }

}
