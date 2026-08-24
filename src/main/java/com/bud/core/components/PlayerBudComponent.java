package com.bud.core.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.bud.core.registry.BudRegistry;
import com.bud.feature.chat.conversation.PersistedMemoryEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
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

    private Set<String> budIds;

    private PlayerRef playerRef;

    private String lastKnownWeatherId;

    private Set<String> lastKnownEffectIds = new HashSet<>();

    private ConcurrentLinkedQueue<NPCEntity> currentBuds = new ConcurrentLinkedQueue<>();

    private Set<PersistedMemoryEntry> persistedMemories = new LinkedHashSet<>();

    private Map<String, Set<PersistedMemoryEntry>> persistedLegendaryMemories = new HashMap<>();

    private long nextMemoryId = 1L;

    public PlayerBudComponent() {
        this.budIds = new HashSet<>();
    }

    public PlayerBudComponent(PlayerRef playerRef) {
        this.budIds = new HashSet<>();
        this.playerRef = playerRef;
    }

    public PlayerBudComponent(PlayerBudComponent clone) {
        this.currentBuds = new ConcurrentLinkedQueue<>(clone.currentBuds);
        this.budIds = new HashSet<>(clone.budIds);
        this.playerRef = clone.playerRef;
        this.lastKnownWeatherId = clone.lastKnownWeatherId;
        this.lastKnownEffectIds = new HashSet<>(clone.lastKnownEffectIds);
        this.persistedMemories = new LinkedHashSet<>(clone.persistedMemories);
        this.persistedLegendaryMemories = new HashMap<>(clone.persistedLegendaryMemories);
        this.nextMemoryId = clone.nextMemoryId;
    }

    @Nonnull
    public static final BuilderCodec<PlayerBudComponent> CODEC = BuilderCodec
            .builder(
                    PlayerBudComponent.class,
                    PlayerBudComponent::new)
            .append(
                    new KeyedCodec<>("BudTypes", new SetCodec<>(Codec.STRING, HashSet::new, false)),
                    (component, value) -> component.budIds = value != null
                            ? value.stream().map(PlayerBudComponent::normalizeAndLogMigration)
                                    .collect(Collectors.toCollection(HashSet::new))
                            : new HashSet<>(),
                    component -> component.budIds)
            .add()
            .append(
                    new KeyedCodec<>("PersistedMemories",
                            new SetCodec<>(PersistedMemoryEntry.CODEC, LinkedHashSet::new, false)),
                    (component, value) -> component.persistedMemories = value != null ? new LinkedHashSet<>(value)
                            : new LinkedHashSet<>(),
                    component -> component.persistedMemories)
            .add()
            .append(
                    new KeyedCodec<>("PersistedLegendaryMemories",
                            new MapCodec<>(new SetCodec<>(PersistedMemoryEntry.CODEC, LinkedHashSet::new, false),
                                    HashMap::new)),
                    (component, value) -> component.persistedLegendaryMemories = value != null ? new HashMap<>(value)
                            : new HashMap<>(),
                    component -> component.persistedLegendaryMemories)
            .add()
            .append(
                    new KeyedCodec<>("NextMemoryId", Codec.LONG),
                    (component, value) -> component.nextMemoryId = value != null ? value : 1L,
                    component -> component.nextMemoryId)
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

    public synchronized void addBud(NPCEntity bud, String budId) {
        pruneInvalidBuds();
        if (currentBuds.size() >= 3) {
            return;
        }
        String normalizedId = BudRegistry.normalize(budId);
        LoggerUtil.getLogger().fine(() -> "[BUD] Adding Bud with id: " + normalizedId);
        currentBuds.add(bud);
        budIds.add(normalizedId);
    }

    public ConcurrentLinkedQueue<NPCEntity> getCurrentBuds() {
        pruneInvalidBuds();
        return currentBuds;
    }

    public synchronized void removeCurrentBud(NPCEntity bud, String budId) {
        String normalizedId = BudRegistry.normalize(budId);
        LoggerUtil.getLogger().fine(() -> "[BUD] Removing Bud with id: " + normalizedId);
        currentBuds.remove(bud);
        budIds.remove(normalizedId);
    }

    public synchronized boolean hasBuds() {
        pruneInvalidBuds();
        if (currentBuds.size() != budIds.size()) {
            LoggerUtil.getLogger().severe(() -> "[BUD] Player has no buds or mismatched bud ids.");
        }
        return !currentBuds.isEmpty();
    }

    @Nonnull
    public Set<String> getBudIds() {
        pruneInvalidBuds();
        return new HashSet<>(budIds);
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

    @Nonnull
    public synchronized Set<String> resolveNewEffectIds(@Nonnull Set<String> currentEffectIds) {
        Set<String> added = new HashSet<>(currentEffectIds);
        added.removeAll(lastKnownEffectIds);
        lastKnownEffectIds = new HashSet<>(currentEffectIds);
        return added;
    }

    public synchronized void setLastKnownEffectIds(@Nonnull Set<String> effectIds) {
        this.lastKnownEffectIds = new HashSet<>(effectIds);
    }

    @Nonnull
    public synchronized Set<PersistedMemoryEntry> getPersistedMemories() {
        return new LinkedHashSet<>(persistedMemories);
    }

    public synchronized void setPersistedMemories(@Nonnull Set<PersistedMemoryEntry> memories) {
        this.persistedMemories = new LinkedHashSet<>(memories);
    }

    @Nonnull
    public synchronized Map<String, Set<PersistedMemoryEntry>> getPersistedLegendaryMemories() {
        return new HashMap<>(persistedLegendaryMemories);
    }

    public synchronized void setPersistedLegendaryMemories(@Nonnull Map<String, Set<PersistedMemoryEntry>> memories) {
        this.persistedLegendaryMemories = new HashMap<>(memories);
    }

    public synchronized long getNextMemoryId() {
        return this.nextMemoryId;
    }

    public synchronized void setNextMemoryId(long nextMemoryId) {
        this.nextMemoryId = nextMemoryId;
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
            String budId = resolveBudId(bud);
            if (budId != null) {
                budIds.remove(budId);
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

    private static String normalizeAndLogMigration(String rawBudId) {
        String normalized = BudRegistry.normalize(rawBudId);
        if (!normalized.equals(rawBudId)) {
            LoggerUtil.getLogger().info(() -> "[BUD] Migrated legacy 'BudTypes' entry '" + rawBudId
                    + "' to bud id '" + normalized + "' on load.");
        }
        return normalized;
    }

    private static String resolveBudId(NPCEntity bud) {
        if (bud == null) {
            return null;
        }
        try {
            String npcTypeId = bud.getNPCTypeId();
            for (String budId : BudRegistry.getInstance().getIds()) {
                if (BudRegistry.getInstance().get(Objects.requireNonNull(budId)).getNpcTypeId().equals(npcTypeId)) {
                    return budId;
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
