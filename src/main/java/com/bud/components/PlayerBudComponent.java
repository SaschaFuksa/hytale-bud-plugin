package com.bud.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerBudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerBudComponent> TYPE;

    private String[] loadedBuds = ArrayUtil.EMPTY_STRING_ARRAY;

    public static void setComponentType(ComponentType<EntityStore, PlayerBudComponent> type) {
        TYPE = type;
    }

    public static ComponentType<EntityStore, PlayerBudComponent> getComponentType() {
        return TYPE;
    }

    public static final BuilderCodec<PlayerBudComponent> CODEC = BuilderCodec
            .builder(PlayerBudComponent.class, PlayerBudComponent::new)
            .<String[]>append(
                    new KeyedCodec<>("LoadedBuds", Codec.STRING_ARRAY),
                    (component, value) -> component.loadedBuds = value,
                    component -> component.loadedBuds)
            .add()
            .build();

    public void setLoadedBuds(String[] budIds) {
        this.loadedBuds = budIds;
    }

    public String[] getLoadedBuds() {
        return loadedBuds;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<EntityStore> clone() {
        try {
            PlayerBudComponent clone = (PlayerBudComponent) super.clone();
            clone.setLoadedBuds(this.loadedBuds);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
