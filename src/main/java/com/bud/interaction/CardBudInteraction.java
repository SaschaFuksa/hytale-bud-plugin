package com.bud.interaction;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.bud.creation.BudCreationEvent;
import com.bud.feature.sound.SoundEvent;
import com.bud.feature.util.CleanupUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class CardBudInteraction extends SimpleInteraction {

    private static final String ALL_BUDS_ID = "roster";

    @Nonnull
    public static final BuilderCodec<CardBudInteraction> CODEC_CARD_BUD = BuilderCodec
            .builder(CardBudInteraction.class, CardBudInteraction::new, SimpleInteraction.CODEC)
            .append(
                    new KeyedCodec<>("BudId", Codec.STRING),
                    (interaction, value) -> interaction.budId = value != null ? value : "",
                    interaction -> interaction.budId)
            .add()
            .build();

    @Nonnull
    private String budId = "";

    public CardBudInteraction() {
        super("card_bud");
    }

    @Nonnull
    public String getBudId() {
        return budId;
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        if (firstRun) {
            Ref<EntityStore> owningEntityRef = context.getOwningEntity();
            if (owningEntityRef == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] Owning entity not present for " + budId);
                return;
            }
            Store<EntityStore> store = owningEntityRef.getStore();
            PlayerRef playerRef = store.getComponent(owningEntityRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                LoggerUtil.getLogger().warning(() -> "[BUD] PlayerRef not present for " + budId);
                return;
            }
            Set<String> budIds = resolveBudIds();
            if (budIds.isEmpty()) {
                LoggerUtil.getLogger().warning(() -> "[BUD] No Buds resolved for card '" + budId + "'.");
                return;
            }
            Set<String> blocked = new HashSet<>(budIds);
            blocked.retainAll(resolveWorkstationBoundBudIds(store, owningEntityRef));
            if (!blocked.isEmpty()) {
                ChatEvent.dispatch(playerRef, describeBlocked(blocked));
            }
            Set<String> allowedBudIds = new HashSet<>(budIds);
            allowedBudIds.removeAll(blocked);
            if (allowedBudIds.isEmpty()) {
                return;
            }
            if (type == InteractionType.Primary) {
                LoggerUtil.getLogger()
                        .info(() -> "[BUD] Spawning " + allowedBudIds + " for " + playerRef.getUsername());
                SoundEvent.dispatch(owningEntityRef, "SFX_Deployable_Totem_Heal_Spawn");
                BudCreationEvent.dispatch(owningEntityRef, allowedBudIds);
            } else if (type == InteractionType.Secondary) {
                LoggerUtil.getLogger()
                        .info(() -> "[BUD] Despawning " + allowedBudIds + " for " + playerRef.getUsername());
                SoundEvent.dispatch(owningEntityRef, "SFX_Deployable_Totem_Heal_Despawn");
                CleanupUtil.cleanupBuds(playerRef, store, allowedBudIds);
            }
        }
        super.tick0(firstRun, time, type, context, cooldownHandler);
    }

    @Nonnull
    private static Set<String> resolveWorkstationBoundBudIds(@Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> playerEntityRef) {
        PlayerBudComponent playerBuds = store.getComponent(playerEntityRef, PlayerBudComponent.getComponentType());
        if (playerBuds == null) {
            return Objects.requireNonNull(Set.of());
        }
        Set<String> bound = new HashSet<>();
        for (NPCEntity bud : playerBuds.getCurrentBuds()) {
            BudComponent component = BudManager.getInstance().findBudComponent(bud);
            if (component != null && component.getWorkstationAnchor() != null) {
                bound.add(component.getBudId());
            }
        }
        return bound;
    }

    @Nonnull
    private static String describeBlocked(@Nonnull Set<String> blockedBudIds) {
        StringBuilder names = new StringBuilder();
        for (String blockedBudId : blockedBudIds) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(BudRegistry.getInstance().get(Objects.requireNonNull(blockedBudId)).getDisplayName());
        }
        return names + " is bound to a Workstation - remove the card from the Workstation first.";
    }

    @Nonnull
    private Set<String> resolveBudIds() {
        if (ALL_BUDS_ID.equalsIgnoreCase(budId)) {
            return Objects.requireNonNull(Set.copyOf(BudRegistry.getInstance().getDefaultBudIds()));
        }
        return Objects.requireNonNull(Set.of(budId));
    }

}
