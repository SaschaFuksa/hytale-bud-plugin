package com.bud.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.BudState;
import static com.bud.core.types.BudState.PET_DEFENSIVE;
import com.bud.feature.world.WorldResolver;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudManager {

    private static final BudManager INSTANCE = new BudManager();

    private final Set<PlayerRef> trackedPlayers = ConcurrentHashMap.newKeySet();

    private BudManager() {
    }

    public static BudManager getInstance() {
        return INSTANCE;
    }

    public static boolean playerHasValidBud(@Nonnull PlayerBudComponent playerBudComponent, @Nonnull String budId) {
        if (playerBudComponent.getBudIds().contains(budId)) {
            String npcTypeId = BudRegistry.getInstance().get(budId).getNpcTypeId();
            Optional<NPCEntity> existingBud = playerBudComponent.getCurrentBuds().stream()
                    .filter(b -> b.getNPCTypeId().equals(npcTypeId))
                    .findFirst();
            if (existingBud.isPresent()) {
                Ref<EntityStore> ref = existingBud.get().getReference();
                return isValidBud(ref);
            }
        }
        return false;
    }

    @Nonnull
    public BudState getNextState(BudState currentState) {
        return switch (currentState) {
            case PET_DEFENSIVE -> BudState.PET_PASSIVE;
            case PET_PASSIVE -> BudState.PET_SITTING;
            case PET_SITTING -> PET_DEFENSIVE;
            default -> BudState.PET_PASSIVE;
        };
    }

    @Nullable
    public BudComponent getRandomBudComponent(@Nullable PlayerBudComponent playerBudComponent) {
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            return null;
        }
        ConcurrentLinkedQueue<NPCEntity> buds = playerBudComponent.getCurrentBuds();
        int size = buds.size();
        if (size == 0) {
            return null;
        }
        int startIndex = ThreadLocalRandom.current().nextInt(size);
        int index = 0;
        for (NPCEntity bud : buds) {
            if (index++ < startIndex) {
                continue;
            }
            BudComponent budComponent = findBudComponent(bud);
            if (isEligibleForReaction(budComponent)) {
                return budComponent;
            }
        }
        for (NPCEntity bud : buds) {
            BudComponent budComponent = findBudComponent(bud);
            if (isEligibleForReaction(budComponent)) {
                return budComponent;
            }
        }
        return null;
    }

    @Nullable
    public BudComponent getRandomOtherBud(PlayerBudComponent playerBudComponent, BudComponent excluded) {
        List<BudComponent> candidates = new ArrayList<>();
        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            BudComponent budComponent = findBudComponent(bud);
            if (isOtherBud(budComponent, excluded)) {
                candidates.add(budComponent);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // Buds currently working never author combat/ambient/etc. reactions - see
    // docs/bud-worker-mode-plan.md "Working-State / Kampf-Lock". Every reaction-trigger filter system
    // picks its speaking Bud through this or getRandomOtherBud, so gating here covers all of them at once.
    private static boolean isEligibleForReaction(@Nullable BudComponent candidate) {
        return candidate != null && candidate.getCurrentState() != BudState.WORKING;
    }

    @Nullable
    public BudComponent findBudByNameMention(@Nonnull PlayerBudComponent playerBudComponent, @Nonnull String message,
            @Nonnull BudComponent excludeSpeaker) {
        String lowerMessage = message.toLowerCase();
        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            BudComponent candidate = findBudComponent(bud);
            if (candidate == null || candidate == excludeSpeaker) {
                continue;
            }
            String displayName = BudRegistry.getInstance().get(candidate.getBudId()).getDisplayName();
            if (lowerMessage.contains(displayName.toLowerCase())) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isOtherBud(BudComponent candidate, BudComponent excluded) {
        return isEligibleForReaction(candidate) && candidate != excluded;
    }

    @Nullable
    public BudComponent findBudComponent(NPCEntity bud) {
        if (bud == null) {
            return null;
        }
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null || !isValidBud(ref)) {
            return null;
        }
        return ref.getStore().getComponent(ref, BudComponent.getComponentType());
    }

    @Nonnull
    public BudComponent getBudComponent(NPCEntity bud) {
        BudComponent component = findBudComponent(bud);
        if (component == null) {
            throw new IllegalStateException("Invalid Bud reference");
        }
        return component;
    }

    private static final int MAX_SPAWN_POSITION_ATTEMPTS = 8;

    private static final int[] FRONT_SPAWN_DISTANCES = { 3, 2, 1 };

    private static final double FRONT_SPAWN_FAN_SPACING = 1.5;

    private static final double MIN_SPAWN_SEPARATION = 0.9;

    private static final double MIN_SPAWN_SEPARATION_SQ = MIN_SPAWN_SEPARATION * MIN_SPAWN_SEPARATION;

    private static final int LATERAL_SEARCH_RADIUS = 2;

    private static final double LATERAL_SEARCH_STEP = 1.0;

    @Nonnull
    public Vector3d getSpawnPosition(@Nonnull PlayerRef playerRef, int index, int total) {
        return getSpawnPosition(playerRef, index, total, new HashSet<>());
    }

    @Nonnull
    public Vector3d getSpawnPosition(@Nonnull PlayerRef playerRef, int index, int total,
            @Nonnull Set<Vector3d> reservedPositions) {
        Vector3d frontPosition = getSpawnPositionInFrontOfPlayer(playerRef, index, total, reservedPositions);
        Vector3d position = frontPosition != null ? frontPosition
                : getPlayerPositionWithOffset(playerRef, reservedPositions);
        reservedPositions.add(position);
        return position;
    }

    @Nullable
    public Vector3d getSpawnPositionInFrontOfPlayer(@Nonnull PlayerRef playerRef, int index, int total,
            @Nonnull Set<Vector3d> reservedPositions) {
        World world = WorldResolver.resolveWithDefaultFallback(playerRef).orElse(null);
        if (world == null) {
            return null;
        }
        Vector3d playerPos = getPlayerPosition(playerRef);
        Rotation3f rotation = playerRef.getTransform().getRotation();
        float yaw = rotation.yaw();
        Vector3d forward = Objects.requireNonNull(Transform.getDirection(0f, yaw));
        Vector3d right = Objects.requireNonNull(Transform.getDirection(0f, yaw - (float) (Math.PI / 2)));
        double preferredLateral = (index - (total - 1) / 2.0) * FRONT_SPAWN_FAN_SPACING;
        for (int distance : FRONT_SPAWN_DISTANCES) {
            Vector3d candidate = findFreeLateralPosition(world, playerPos, forward, right, distance, preferredLateral,
                    reservedPositions);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private static Vector3d findFreeLateralPosition(@Nonnull World world, @Nonnull Vector3d playerPos,
            @Nonnull Vector3d forward, @Nonnull Vector3d right, int distance, double preferredLateral,
            @Nonnull Set<Vector3d> reservedPositions) {
        Vector3d center = candidateAt(world, playerPos, forward, right, distance, preferredLateral,
                reservedPositions);
        if (center != null) {
            return center;
        }
        for (int step = 1; step <= LATERAL_SEARCH_RADIUS; step++) {
            Vector3d rightCandidate = candidateAt(world, playerPos, forward, right, distance,
                    preferredLateral + step * LATERAL_SEARCH_STEP, reservedPositions);
            if (rightCandidate != null) {
                return rightCandidate;
            }
            Vector3d leftCandidate = candidateAt(world, playerPos, forward, right, distance,
                    preferredLateral - step * LATERAL_SEARCH_STEP, reservedPositions);
            if (leftCandidate != null) {
                return leftCandidate;
            }
        }
        return null;
    }

    @Nullable
    private static Vector3d candidateAt(@Nonnull World world, @Nonnull Vector3d playerPos, @Nonnull Vector3d forward,
            @Nonnull Vector3d right, int distance, double lateralOffset, @Nonnull Set<Vector3d> reservedPositions) {
        double x = playerPos.x + forward.x * distance + right.x * lateralOffset;
        double y = playerPos.y + 0.5;
        double z = playerPos.z + forward.z * distance + right.z * lateralOffset;
        Vector3d candidate = new Vector3d(x, y, z);
        return isSpawnPositionFree(world, x, y, z) && !isReserved(candidate, reservedPositions) ? candidate : null;
    }

    @Nullable
    public Vector3d getBudPosition(@Nonnull NPCEntity bud) {
        Ref<EntityStore> ref = bud.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        ComponentType<EntityStore, TransformComponent> transformComponentType = TransformComponent.getComponentType();
        if (transformComponentType == null) {
            return null;
        }
        TransformComponent transformComponent = ref.getStore().getComponent(ref, transformComponentType);
        return transformComponent != null ? transformComponent.getPosition() : null;
    }

    private static boolean isReserved(@Nonnull Vector3d candidate, @Nonnull Set<Vector3d> reservedPositions) {
        for (Vector3d reserved : reservedPositions) {
            if (candidate.distanceSquared(reserved) < MIN_SPAWN_SEPARATION_SQ) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public Vector3f getRotationFacingPlayer(@Nonnull PlayerRef playerRef, @Nonnull Vector3d budPosition) {
        Vector3d playerPos = getPlayerPosition(playerRef);
        float playerYaw = playerRef.getTransform().getRotation().yaw();
        Vector3d forward = Objects.requireNonNull(Transform.getDirection(0f, playerYaw));
        Vector3d right = Objects.requireNonNull(Transform.getDirection(0f, playerYaw - (float) (Math.PI / 2)));
        double toPlayerX = playerPos.x - budPosition.x;
        double toPlayerZ = playerPos.z - budPosition.z;
        double forwardComponent = toPlayerX * forward.x + toPlayerZ * forward.z;
        double rightComponent = toPlayerX * right.x + toPlayerZ * right.z;
        float budYaw = playerYaw + (float) Math.atan2(-rightComponent, forwardComponent);
        return new Vector3f(0f, budYaw, 0f);
    }

    @Nonnull
    public Vector3d getPlayerPositionWithOffset(@Nonnull PlayerRef playerRef) {
        return getPlayerPositionWithOffset(playerRef, new HashSet<>());
    }

    @Nonnull
    public Vector3d getPlayerPositionWithOffset(@Nonnull PlayerRef playerRef, @Nonnull Set<Vector3d> reservedPositions) {
        Vector3d targetPos = getPlayerPosition(playerRef);
        World world = WorldResolver.resolveWithDefaultFallback(playerRef).orElse(null);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < MAX_SPAWN_POSITION_ATTEMPTS; attempt++) {
            double offsetX = targetPos.x + random.nextDouble() * 6 - 3;
            double offsetY = targetPos.y + 0.5;
            double offsetZ = targetPos.z + random.nextDouble() * 6 - 3;
            Vector3d candidate = new Vector3d(offsetX, offsetY, offsetZ);
            boolean free = world == null || isSpawnPositionFree(world, offsetX, offsetY, offsetZ);
            if (free && !isReserved(candidate, reservedPositions)) {
                return candidate;
            }
        }
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] No free spawn position found near player after "
                        + MAX_SPAWN_POSITION_ATTEMPTS + " attempts, falling back to player position");
        return new Vector3d(targetPos.x, targetPos.y + 0.5, targetPos.z);
    }

    private static boolean isSpawnPositionFree(@Nonnull World world, double x, double y, double z) {
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        return world.getBlock(blockX, blockY, blockZ) == BlockType.EMPTY_ID
                && world.getBlock(blockX, blockY + 1, blockZ) == BlockType.EMPTY_ID;
    }

    @Nonnull
    public Vector3d getPlayerPosition(@Nonnull PlayerRef playerRef) {
        return Objects.requireNonNull(playerRef.getTransform().getPosition());
    }

    public void registerPlayer(@Nonnull PlayerRef playerRef) {
        trackedPlayers.add(playerRef);
    }

    public void unregisterPlayer(@Nonnull PlayerRef playerRef) {
        trackedPlayers.remove(playerRef);
    }

    public Set<PlayerRef> getTrackedPlayers() {
        return Set.copyOf(trackedPlayers);
    }

    public Set<BudComponent> getAllBuds() {
        World world = WorldResolver.resolveDefaultWorld().orElse(null);
        if (world == null) {
            return Set.of();
        }
        return getAllBuds(world);
    }

    public Set<BudComponent> getAllBuds(@Nonnull World world) {
        try {
            return collectAllBudComponents(world);
        } catch (IllegalStateException exception) {
            if (!isThreadAssertion(exception)) {
                throw exception;
            }
            LoggerUtil.getLogger().finer(() -> "[BUD] getAllBuds called off world thread. Retrying on world thread.");
            return executeOnWorldThread(world, () -> collectAllBudComponents(world));
        }
    }

    public Set<PlayerBudComponent> getAllPlayers() {
        World world = WorldResolver.resolveDefaultWorld().orElse(null);
        if (world == null) {
            return Set.of();
        }
        return getAllPlayers(world);
    }

    public Set<PlayerBudComponent> getAllPlayers(@Nonnull World world) {
        try {
            return collectAllPlayerComponents(world);
        } catch (IllegalStateException exception) {
            if (!isThreadAssertion(exception)) {
                throw exception;
            }
            LoggerUtil.getLogger()
                    .finer(() -> "[BUD] getAllPlayers called off world thread. Retrying on world thread.");
            return executeOnWorldThread(world, () -> collectAllPlayerComponents(world));
        }
    }

    private static Set<BudComponent> collectAllBudComponents(World world) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ConcurrentLinkedQueue<BudComponent> allBudComponents = new ConcurrentLinkedQueue<>();
        entityStore.forEachEntityParallel(
                BudComponent.getComponentType(),
                (index, archetypeChunk, commandBuffer) -> {
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Checking entity for cleanup: " + index);
                    BudComponent budComponent = archetypeChunk.getComponent(index,
                            BudComponent.getComponentType());
                    if (budComponent == null) {
                        return;
                    }
                    allBudComponents.add(budComponent);
                });
        return Set.copyOf(allBudComponents);
    }

    private static Set<PlayerBudComponent> collectAllPlayerComponents(World world) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ConcurrentLinkedQueue<PlayerBudComponent> allPlayerBudComponents = new ConcurrentLinkedQueue<>();
        entityStore.forEachEntityParallel(
                PlayerBudComponent.getComponentType(),
                (index, archetypeChunk, commandBuffer) -> {
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Checking entity for cleanup: " + index);
                    PlayerBudComponent playerBudComponent = archetypeChunk.getComponent(index,
                            PlayerBudComponent.getComponentType());
                    if (playerBudComponent == null) {
                        return;
                    }
                    allPlayerBudComponents.add(playerBudComponent);
                });
        return Set.copyOf(allPlayerBudComponents);
    }

    private static <T> Set<T> executeOnWorldThread(World world, Supplier<Set<T>> supplier) {
        CompletableFuture<Set<T>> future = new CompletableFuture<>();
        try {
            world.execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Exception exception) {
                    future.completeExceptionally(exception);
                }
            });
            return future.join();
        } catch (Exception exception) {
            LoggerUtil.getLogger().warning(
                    () -> "[BUD] Could not execute entity query on world thread: " + exception.getMessage());
            return Set.of();
        }
    }

    private static boolean isThreadAssertion(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("Assert not in thread");
    }

    private static boolean isValidBud(Ref<EntityStore> budRef) {
        if (budRef != null && budRef.isValid()) {
            ComponentType<EntityStore, DeathComponent> deathComponentType = DeathComponent.getComponentType();
            if (deathComponentType == null) {
                return true;
            }
            return !budRef.getStore().getArchetype(budRef).contains(deathComponentType);
        }
        return false;
    }

}