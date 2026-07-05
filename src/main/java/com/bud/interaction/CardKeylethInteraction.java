package com.bud.interaction;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CardKeylethInteraction extends SimpleInteraction {

    public static final BuilderCodec<CardKeylethInteraction> CODEC = BuilderCodec
            .builder(CardKeylethInteraction.class, CardKeylethInteraction::new, SimpleInteraction.CODEC).build();

    public CardKeylethInteraction() {
        super("card_keyleth");
    }

    @Override
    protected void tick0(boolean firstRun, float time, InteractionType type, InteractionContext context,
            CooldownHandler cooldownHandler) {
        LoggerUtil.getLogger()
                .info("[BUD] CardKeylethInteraction tick0 called with firstRun: " + firstRun + ", time: " + time
                        + ", type: " + type);
        Ref<EntityStore> owningEntityRef = context.getOwningEntity();
        PlayerRef playerRef = owningEntityRef.getStore().getComponent(owningEntityRef, PlayerRef.getComponentType());
        LoggerUtil.getLogger().info("[BUD] Owning entity: " + playerRef.getUsername());
        super.tick0(firstRun, time, type, context, cooldownHandler);
    }

}
