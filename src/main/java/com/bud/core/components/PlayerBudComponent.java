package com.bud.core.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nonnull;

import com.bud.core.types.BudType;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class PlayerBudComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PlayerBudComponent> TYPE;

    private Set<BudType> budTypes;

    private PlayerRef playerRef;

    private String lastKnownWeatherId;

    private ConcurrentLinkedQueue<NPCEntity> currentBuds = new ConcurrentLinkedQueue<>();

    public PlayerBudComponent() {
        this.budTypes = new HashSet<>();
    }

    public PlayerBudComponent(PlayerRef playerRef) {
        this.budTypes = new HashSet<>();
        this.playerRef = playerRef;
    }

    public PlayerBudComponent(PlayerBudComponent clone) {
        this.currentBuds = new ConcurrentLinkedQueue<>(clone.currentBuds);
        this.budTypes = new HashSet<>(clone.budTypes);
        this.playerRef = clone.playerRef;
        this.lastKnownWeatherId = clone.lastKnownWeatherId;
    }

    @Nonnull
    public static final BuilderCodec<PlayerBudComponent> CODEC = BuilderCodec
            .builder(
                    PlayerBudComponent.class,
                    PlayerBudComponent::new)
            .append(
                    new KeyedCodec<>("BudTypes", new SetCodec<>(new EnumCodec<>(BudType.class), HashSet::new, false)),
                    (component, value) -> component.budTypes = value != null ? new HashSet<>(value) : new HashSet<>(),
                    component -> component.budTypes)
            .add()
            .build();

    public static void setComponentType(ComponentType<EntityStore, PlayerBudComponent> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<EntityStore, PlayerBudComponent> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getEntityStoreRegistry().registerComponent(
                    PlayerBudComponent.class,
                    "PlayerBudComponent",
                    PlayerBudComponent.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    public synchronized void addBud(NPCEntity bud, BudType budType) {
        pruneInvalidBuds();
        if (currentBuds.size() >= 3) {
            return;
        }
        LoggerUtil.getLogger().fine(() -> "[BUD] Adding Bud with NPC Type ID: " + budType.getName());
        currentBuds.add(bud);
        budTypes.add(budType);
    }

    public ConcurrentLinkedQueue<NPCEntity> getCurrentBuds() {
        pruneInvalidBuds();
        return currentBuds;
    }

    public synchronized void removeCurrentBud(NPCEntity bud, BudType budType) {
        LoggerUtil.getLogger().fine(() -> "[BUD] Removing Bud with NPC Type ID: " + budType.getName());
        currentBuds.remove(bud);
        budTypes.remove(budType);
    }

    public synchronized boolean hasBuds() {
        pruneInvalidBuds();
        if (currentBuds.size() != budTypes.size()) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Player has no buds or mismatched bud types.");
        }
        return !currentBuds.isEmpty();
    }

    @Nonnull
    public Set<BudType> getBudTypes() {
        pruneInvalidBuds();
        return new HashSet<>(budTypes);
    }

    @Nonnull
    public PlayerRef getPlayerRef() {
        if (playerRef == null) {
            throw new IllegalStateException("PlayerRef cannot be null in PlayerBudComponent");
        }
        return playerRef;
    }

    public void setPlayerRef(@Nonnull PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public synchronized boolean updateWeatherIfChanged(String weatherId) {
        if (weatherId == null || weatherId.isBlank()) {
            return false;
        }
        if (weatherId.equals(lastKnownWeatherId)) {
            return false;
        }
        lastKnownWeatherId = weatherId;
        return true;
    }

    public synchronized String getLastKnownWeatherId() {
        return lastKnownWeatherId;
    }

    public synchronized void setLastKnownWeatherId(String weatherId) {
        this.lastKnownWeatherId = weatherId;
    }

    private synchronized void pruneInvalidBuds() {
        List<NPCEntity> invalidBuds = new ArrayList<>();
        for (NPCEntity bud : currentBuds) {
            if (isBudReferenceValid(bud)) {
                continue;
            }
            invalidBuds.add(bud);
        }
        for (NPCEntity bud : invalidBuds) {
            currentBuds.remove(bud);
            BudType budType = resolveBudType(bud);
            if (budType != null) {
                budTypes.remove(budType);
            }
        }
    }

    private static boolean isBudReferenceValid(NPCEntity bud) {
        if (bud == null) {
            return false;
        }
        try {
            Ref<EntityStore> budRef = bud.getReference();
            return budRef != null && budRef.isValid();
        } catch (Exception exception) {
            return false;
        }
    }

    private static BudType resolveBudType(NPCEntity bud) {
        if (bud == null) {
            return null;
        }
        try {
            String npcTypeId = bud.getNPCTypeId();
            for (BudType budType : BudType.values()) {
                if (budType.getName().equals(npcTypeId)) {
                    return budType;
                }
            }
        } catch (Exception exception) {
            return null;
        }
        return null;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<EntityStore> clone() {
        try {
            return (PlayerBudComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
