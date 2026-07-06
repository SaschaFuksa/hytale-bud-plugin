package com.bud.interaction;

import com.bud.core.types.BudType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class CardGronkhInteraction extends CardBudInteraction {

        public static final BuilderCodec<CardGronkhInteraction> CODEC_CARD_GRONKH = BuilderCodec
            .builder(CardGronkhInteraction.class, CardGronkhInteraction::new, SimpleInteraction.CODEC).build();

    public CardGronkhInteraction() {
        super("card_gronkh", BudType.GRONKH);
    }

}
