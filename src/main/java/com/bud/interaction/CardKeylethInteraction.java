package com.bud.interaction;

import com.bud.core.types.BudType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class CardKeylethInteraction extends CardBudInteraction {

    public static final BuilderCodec<CardKeylethInteraction> CODEC_CARD_KEYLETH = BuilderCodec
            .builder(CardKeylethInteraction.class, CardKeylethInteraction::new, SimpleInteraction.CODEC).build();

    public CardKeylethInteraction() {
        super("card_keyleth", BudType.KEYLETH);
    }

}
