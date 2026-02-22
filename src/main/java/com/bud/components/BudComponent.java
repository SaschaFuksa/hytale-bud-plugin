package com.bud.components;

import javax.annotation.Nonnull;

import com.bud.profile.BudType;
import com.bud.reaction.state.BudState;
import com.bud.reaction.world.time.Mood;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, BudComponent> TYPE;

    private BudState currentState = BudState.PET_DEFENSIVE;

    private Mood currentMood = Mood.DEFAULT;

    @Nonnull
    private BudType budType = BudType.VERI;

    private NPCEntity bud;

    private PlayerRef playerRef;

    /**
     * Default constructor for Codec deserialization.
     * Use {@link #create(NPCEntity, PlayerRef)} for runtime instantiation.
     */
    public BudComponent() {
    }

    /**
     * Factory method for creating a properly initialized BudComponent.
     */
    @Nonnull
    public static BudComponent create(@Nonnull NPCEntity bud, @Nonnull BudType budType, @Nonnull PlayerRef playerRef) {
        BudComponent component = new BudComponent();
        component.bud = bud;
        component.playerRef = playerRef;
        component.budType = budType;
        return component;
    }

    @Nonnull
    public static final BuilderCodec<BudComponent> CODEC = BuilderCodec.builder(BudComponent.class, BudComponent::new)
            .build();

    public static void setComponentType(ComponentType<EntityStore, BudComponent> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<EntityStore, BudComponent> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getEntityStoreRegistry().registerComponent(
                    BudComponent.class,
                    "BudComponent",
                    BudComponent.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    public void setCurrentState(BudState state) {
        this.currentState = state;
    }

    public BudState getCurrentState() {
        return currentState;
    }

    @Nonnull
    public BudType getBudType() {
        return budType;
    }

    @Nonnull
    public NPCEntity getBud() {
        if (bud == null) {
            throw new IllegalStateException("NPCEntity cannot be null in BudComponent");
        }
        return bud;
    }

    @Nonnull
    public PlayerRef getPlayerRef() {
        if (playerRef == null) {
            throw new IllegalStateException("PlayerRef cannot be null in BudComponent");
        }
        return playerRef;
    }

    public void setCurrentMood(Mood mood) {
        this.currentMood = mood;
    }

    public Mood getCurrentMood() {
        return currentMood;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<EntityStore> clone() {
        try {
            return (BudComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
