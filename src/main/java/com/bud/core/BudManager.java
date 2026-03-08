package com.bud.core;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.BudState;
import static com.bud.core.types.BudState.PET_DEFENSIVE;
import com.bud.core.types.BudType;
import com.bud.feature.world.WorldResolver;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
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

    public static boolean playerHasValidBud(@Nonnull PlayerBudComponent playerBudComponent, @Nonnull BudType budType) {
        if (playerBudComponent.getBudTypes().contains(budType)) {
            Optional<NPCEntity> existingBud = playerBudComponent.getCurrentBuds().stream()
                    .filter(b -> b.getNPCTypeId().equals(budType.getName()))
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

    public BudComponent getRandomBudComponent(PlayerBudComponent playerBudComponent) {
        if (!playerBudComponent.hasBuds()) {
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
            if (budComponent != null) {
                return budComponent;
            }
        }
        for (NPCEntity bud : buds) {
            BudComponent budComponent = findBudComponent(bud);
            if (budComponent != null) {
                return budComponent;
            }
        }
        return null;
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

    @Nonnull
    public Vector3d getPlayerPositionWithOffset(PlayerRef playerRef) {
        Vector3d targetPos = getPlayerPosition(playerRef);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double offsetX = targetPos.getX() + random.nextDouble() * 6 - 3;
        double offsetY = targetPos.getY() + 0.5;
        double offsetZ = targetPos.getZ() + random.nextDouble() * 6 - 3;
        return new Vector3d(offsetX, offsetY, offsetZ);
    }

    public Vector3d getPlayerPosition(PlayerRef playerRef) {
        return playerRef.getTransform().getPosition();
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