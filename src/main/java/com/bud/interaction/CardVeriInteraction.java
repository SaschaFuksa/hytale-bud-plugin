package com.bud.interaction;

import com.bud.core.types.BudType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;

public class CardVeriInteraction extends CardBudInteraction {

    public static final BuilderCodec<CardVeriInteraction> CODEC_CARD_VERI = BuilderCodec
            .builder(CardVeriInteraction.class, CardVeriInteraction::new, SimpleInteraction.CODEC).build();

    public CardVeriInteraction() {
        super("card_veri", BudType.VERI);
    }

}
