package com.bud.feature.work;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3i;

import com.bud.core.components.BudComponent;
import com.bud.core.types.WorkRole;
import com.bud.core.types.WorkType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class WorkstationBlockEntity implements Component<ChunkStore> {

    private static ComponentType<ChunkStore, WorkstationBlockEntity> TYPE;

    @Nonnull
    private static final Codec<WorkRole> WORK_ROLE_CODEC = new EnumCodec<>(WorkRole.class);

    @Nonnull
    private WorkRole workRole = WorkRole.FARMING;

    @Nullable
    private UUID ownerPlayerId;

    @Nullable
    private BudComponent boundBud;

    private float fuelSecondsRemaining;

    private boolean resting;

    private transient boolean idleNoWork;

    private float rebindRetrySecondsRemaining;

    private float targetElapsedSeconds;

    private static final int RECENTLY_FAILED_CAPACITY = 4;

    @Nonnull
    private final Deque<Vector3i> recentlyFailedTargets = new ArrayDeque<>(RECENTLY_FAILED_CAPACITY);

    @Nullable
    private Vector3i lastAssignedPosition;

    @Nullable
    private WorkType lastAssignedWorkType;

    private int consecutiveRepeatCount;

    private static final int MAX_BIND_ATTEMPTS = 5;

    private int bindFailureCount;

    @Nullable
    private ItemStack lastAttemptedCard;

    @Nonnull
    private final Set<Vector3i> lockedHarvestOutputTargets = new HashSet<>();

    private long outputFullReactionLastFiredMillis;

    private boolean outOfFuelReactionSent;

    @Nullable
    private List<Vector3i> cachedSerpentinePositions;

    @Nullable
    private Vector3i cachedSerpentineAnchor;

    private int cachedSerpentineRadius = Integer.MIN_VALUE;

    private int cachedSerpentineMaxHeight = Integer.MIN_VALUE;

    @Nullable
    private List<Vector3i> cachedEdgePositions;

    @Nullable
    private Vector3i cachedEdgeAnchor;

    private int cachedEdgeRadius = Integer.MIN_VALUE;

    private int cachedEdgeMaxHeight = Integer.MIN_VALUE;

    private int cachedEdgeCount = Integer.MIN_VALUE;

    @Nonnull
    public static final BuilderCodec<WorkstationBlockEntity> CODEC = BuilderCodec
            .builder(WorkstationBlockEntity.class, WorkstationBlockEntity::new)
            .append(new KeyedCodec<>("WorkRole", WORK_ROLE_CODEC),
                    (entity, value) -> entity.workRole = value != null ? value : WorkRole.FARMING,
                    entity -> entity.workRole)
            .add()
            .append(new KeyedCodec<>("OwnerPlayerId", Codec.UUID_BINARY),
                    (entity, value) -> entity.ownerPlayerId = value,
                    entity -> entity.ownerPlayerId)
            .add()
            .build();

    public static void setComponentType(ComponentType<ChunkStore, WorkstationBlockEntity> type) {
        TYPE = type;
    }

    @Nonnull
    public static ComponentType<ChunkStore, WorkstationBlockEntity> getComponentType() {
        if (TYPE == null) {
            TYPE = Universe.get().getChunkStoreRegistry().registerComponent(
                    WorkstationBlockEntity.class,
                    "WorkstationBlockEntity",
                    WorkstationBlockEntity.CODEC);
            return TYPE;
        }
        return TYPE;
    }

    @Nonnull
    public WorkRole getWorkRole() {
        return workRole;
    }

    public void setWorkRole(@Nonnull WorkRole workRole) {
        this.workRole = workRole;
    }

    @Nullable
    public UUID getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public void setOwnerPlayerId(@Nullable UUID ownerPlayerId) {
        this.ownerPlayerId = ownerPlayerId;
    }

    @Nullable
    public BudComponent getBoundBud() {
        return boundBud;
    }

    public void setBoundBud(@Nullable BudComponent boundBud) {
        this.boundBud = boundBud;
    }

    public float getFuelSecondsRemaining() {
        return fuelSecondsRemaining;
    }

    public void setFuelSecondsRemaining(float fuelSecondsRemaining) {
        this.fuelSecondsRemaining = fuelSecondsRemaining;
    }

    public boolean isResting() {
        return resting;
    }

    public void setResting(boolean resting) {
        this.resting = resting;
    }

    public boolean isIdleNoWork() {
        return idleNoWork;
    }

    public void setIdleNoWork(boolean idleNoWork) {
        this.idleNoWork = idleNoWork;
    }

    public float getRebindRetrySecondsRemaining() {
        return rebindRetrySecondsRemaining;
    }

    public void setRebindRetrySecondsRemaining(float rebindRetrySecondsRemaining) {
        this.rebindRetrySecondsRemaining = rebindRetrySecondsRemaining;
    }

    public float getTargetElapsedSeconds() {
        return targetElapsedSeconds;
    }

    public void setTargetElapsedSeconds(float targetElapsedSeconds) {
        this.targetElapsedSeconds = targetElapsedSeconds;
    }

    public boolean isRecentlyFailedTarget(@Nonnull Vector3i position) {
        return recentlyFailedTargets.contains(position);
    }

    public void addRecentlyFailedTarget(@Nonnull Vector3i position) {
        if (recentlyFailedTargets.size() >= RECENTLY_FAILED_CAPACITY) {
            recentlyFailedTargets.removeFirst();
        }
        recentlyFailedTargets.addLast(position);
    }

    @Nullable
    public Vector3i getLastAssignedPosition() {
        return lastAssignedPosition;
    }

    public void setLastAssignedPosition(@Nullable Vector3i lastAssignedPosition) {
        this.lastAssignedPosition = lastAssignedPosition;
    }

    @Nullable
    public WorkType getLastAssignedWorkType() {
        return lastAssignedWorkType;
    }

    public void setLastAssignedWorkType(@Nullable WorkType lastAssignedWorkType) {
        this.lastAssignedWorkType = lastAssignedWorkType;
    }

    public int getConsecutiveRepeatCount() {
        return consecutiveRepeatCount;
    }

    public void setConsecutiveRepeatCount(int consecutiveRepeatCount) {
        this.consecutiveRepeatCount = consecutiveRepeatCount;
    }

    public int getBindFailureCount() {
        return bindFailureCount;
    }

    public void setBindFailureCount(int bindFailureCount) {
        this.bindFailureCount = bindFailureCount;
    }

    public boolean hasGivenUpBinding() {
        return bindFailureCount >= MAX_BIND_ATTEMPTS;
    }

    @Nullable
    public ItemStack getLastAttemptedCard() {
        return lastAttemptedCard;
    }

    public void setLastAttemptedCard(@Nullable ItemStack lastAttemptedCard) {
        this.lastAttemptedCard = lastAttemptedCard;
    }

    public boolean isHarvestOutputLocked(@Nonnull Vector3i position) {
        return lockedHarvestOutputTargets.contains(position);
    }

    public void lockHarvestOutputTarget(@Nonnull Vector3i position) {
        lockedHarvestOutputTargets.add(position);
    }

    public void clearHarvestOutputLocks() {
        lockedHarvestOutputTargets.clear();
    }

    public boolean hasLockedHarvestOutputTargets() {
        return !lockedHarvestOutputTargets.isEmpty();
    }

    public boolean isOutputFullReactionDue(long cooldownMillis) {
        return System.currentTimeMillis() - outputFullReactionLastFiredMillis >= cooldownMillis;
    }

    public void markOutputFullReactionFired() {
        outputFullReactionLastFiredMillis = System.currentTimeMillis();
    }

    public void onOutputContainerChanged() {
        clearHarvestOutputLocks();
    }

    @Nonnull
    public List<Vector3i> cachedSerpentinePositions(@Nonnull Vector3i flooredAnchor, int radius, int maxHeight,
            @Nonnull Supplier<List<Vector3i>> compute) {
        List<Vector3i> cached = cachedSerpentinePositions;
        if (cached != null && flooredAnchor.equals(cachedSerpentineAnchor) && cachedSerpentineRadius == radius
                && cachedSerpentineMaxHeight == maxHeight) {
            return cached;
        }
        List<Vector3i> computed = Objects.requireNonNull(compute.get());
        cachedSerpentinePositions = computed;
        cachedSerpentineAnchor = flooredAnchor;
        cachedSerpentineRadius = radius;
        cachedSerpentineMaxHeight = maxHeight;
        return computed;
    }

    @Nonnull
    public List<Vector3i> cachedEdgePositions(@Nonnull Vector3i flooredAnchor, int radius, int maxHeight,
            int edgeCount, @Nonnull Supplier<List<Vector3i>> compute) {
        List<Vector3i> cached = cachedEdgePositions;
        if (cached != null && flooredAnchor.equals(cachedEdgeAnchor) && cachedEdgeRadius == radius
                && cachedEdgeMaxHeight == maxHeight && cachedEdgeCount == edgeCount) {
            return cached;
        }
        List<Vector3i> computed = Objects.requireNonNull(compute.get());
        cachedEdgePositions = computed;
        cachedEdgeAnchor = flooredAnchor;
        cachedEdgeRadius = radius;
        cachedEdgeMaxHeight = maxHeight;
        cachedEdgeCount = edgeCount;
        return computed;
    }

    public boolean isOutOfFuelReactionSent() {
        return outOfFuelReactionSent;
    }

    public void setOutOfFuelReactionSent(boolean outOfFuelReactionSent) {
        this.outOfFuelReactionSent = outOfFuelReactionSent;
    }

    public void onFuelContainerChanged() {
        outOfFuelReactionSent = false;
    }

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Component<ChunkStore> clone() {
        try {
            return (WorkstationBlockEntity) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
